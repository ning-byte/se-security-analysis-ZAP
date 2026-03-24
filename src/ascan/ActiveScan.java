@Override
public void hostProgress(int id, String hostAndPort, String msg, int percentage) {

    int tot = 0;
    for (HostProcess process : this.getHostProcesses()) {
        tot += process.getPercentageComplete();
    }
    int latestProgress = tot / this.getHostProcesses().size();
    if (latestProgress != this.progress) {
        this.progress = latestProgress;
        ActiveScanEventPublisher.publishScanProgressEvent(this.getId(), this.progress);
    }
}
@Override
public void notifyNewMessage(final HttpMessage msg) {
    this.rcTotals.incResponseCodeCount(msg.getResponseHeader().getStatusCode());

    if (!persistTemporaryMessages) {
        return;
    }

    HistoryReference hRef = msg.getHistoryRef();
    if (hRef == null) {
        try {
            hRef =
                    new HistoryReference(
                            Model.getSingleton().getSession(),
                            HistoryReference.TYPE_SCANNER_TEMPORARY,
                            msg);
            msg.setHistoryRef(null);
            hRefs.add(hRef.getHistoryId());
        } catch (HttpMalformedHeaderException | DatabaseException e) {
            if (ErrorUtils.handleDiskSpaceException(e)) {
                // Its serious, stop the scans
                this.getHostProcesses().forEach(HostProcess::stop);
                return;
            }
            LOGGER.error(e.getMessage(), e);
        }
    } else {
        hRefs.add(hRef.getHistoryId());
    }

    if (hRef != null && View.isInitialised()) {

        if (this.rcTotals.getTotal() > this.maxResultsToList) {
            removeFirstHistoryReferenceInEdt();
        }
        addHistoryReferenceInEdt(hRef);
    }
}