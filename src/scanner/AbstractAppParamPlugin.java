@Override
public void scan() {
    listVariant =
            getVariantFactory()
                    .createVariants(this.getParent().getScannerParam(), this.getBaseMsg());

    if (listVariant.isEmpty()) {
        getParent()
                .pluginSkipped(
                        this,
                        Constant.messages.getString(
                                "ascan.progress.label.skipped.reason.noinputvectors"));
        return;
    }

    for (int i = 0; i < listVariant.size() && !isStop(); i++) {

        HttpMessage msg = getNewMsg();
        variant = listVariant.get(i);
        try {
            variant.setMessage(msg);
            this.scan(this.variant.getParamList());
        } catch (Exception e) {
            getLogger()
                    .error(
                            "Error occurred while scanning with variant {}",
                            variant.getClass().getCanonicalName(),
                            e);
        }

        while (getParent().isPaused() && !isStop()) {
            Util.sleep(500);
        }
    }
}
private boolean isToExclude(NameValuePair param) {
    if (param.getType() == NameValuePair.TYPE_POST_DATA
            && getParent().getScannerParam().isExcludeAntiCsrfTokens()) {
        if (extensionAntiCsrf == null) {
            extensionAntiCsrf =
                    Control.getSingleton()
                            .getExtensionLoader()
                            .getExtension(ExtensionAntiCSRF.class);
        }
        if (extensionAntiCsrf != null && extensionAntiCsrf.isAntiCsrfToken(param.getName())) {
            return true;
        }
    }

    List<ScannerParamFilter> excludedParameters = getParameterExclusionFilters(param);

    HttpMessage msg = getBaseMsg();

    for (ScannerParamFilter filter : excludedParameters) {
        if (filter.isToExclude(msg, param)) {
            return true;
        }
    }

    return false;
}
public void scan(HttpMessage msg, NameValuePair originalParam) {
    scan(msg, originalParam.getName(), originalParam.getValue());
}
@Override
protected AlertBuilder newAlert() {
    AlertBuilder builder = super.newAlert();
    if (variant != null) {
        builder.setInputVector(variant.getShortName());
    }
    if (originalPair != null) {
        builder.setParam(originalPair.getName());
    }
    return builder;
}
