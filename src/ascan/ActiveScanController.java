@Override
public int startScan(String name, Target target, User user, Object[] contextSpecificObjects) {
    activeScansLock.lock();
    try {
        int id = this.scanIdCounter++;  // 生成唯一扫描ID

        RuleConfigParam ruleConfigParam = null;
        ExtensionRuleConfig extRC =
                Control.getSingleton()
                        .getExtensionLoader()
                        .getExtension(ExtensionRuleConfig.class);
        if (extRC != null) {
            ruleConfigParam = extRC.getRuleConfigParam();
        }

        ActiveScan ascan =
                new ActiveScan(name, extension.getScannerParam(), null, ruleConfigParam) {
                    @Override
                    public void alertFound(Alert alert) {
                        alert.setSource(Alert.Source.ACTIVE);
                        if (extAlert != null) {
                            extAlert.alertFound(alert, null);
                        }
                        super.alertFound(alert);
                    }
                };

        Session session = extension.getModel().getSession();
        List<String> excludeList = new ArrayList<>();
        excludeList.addAll(extension.getExcludeList());
        excludeList.addAll(session.getExcludeFromScanRegexs());
        excludeList.addAll(session.getGlobalExcludeURLRegexs());
        ascan.setExcludeList(excludeList);
        ScanPolicy policy = null;

        ascan.setId(id);
        ascan.setUser(user);

        boolean techOverridden = false;

        if (contextSpecificObjects != null) {
            for (Object obj : contextSpecificObjects) {
                if (obj instanceof ScannerParam) {
                    LOGGER.debug("Setting custom scanner params");
                    ascan.setScannerParam((ScannerParam) obj);
                } else if (obj instanceof ScanPolicy) {
                    policy = (ScanPolicy) obj;
                    LOGGER.debug("Setting custom policy {}", policy.getName());
                    ascan.setScanPolicy(policy);
                } else if (obj instanceof TechSet) {
                    ascan.setTechSet((TechSet) obj);
                    techOverridden = true;
                } else if (obj instanceof ScriptCollection) {
                    ascan.addScriptCollection((ScriptCollection) obj);
                } else if (obj instanceof ScanFilter) {
                    ascan.addScanFilter((ScanFilter) obj);
                } else {
                    LOGGER.error(
                            "Unexpected contextSpecificObject: {}",
                            obj.getClass().getCanonicalName());
                }
            }
        }
        if (policy == null) {

            policy = extension.getPolicyManager().getDefaultScanPolicy();
            LOGGER.debug("Setting default policy {}", policy.getName());
            ascan.setScanPolicy(policy);
        }

        if (!techOverridden && target.getContext() != null) {
            ascan.setTechSet(target.getContext().getTechSet());
        }


        this.activeScanMap.put(id, ascan);
        this.activeScanList.add(ascan);
        ascan.start(target);  // 启动扫描

        return id;
    } finally {
        activeScansLock.unlock();
    }
}