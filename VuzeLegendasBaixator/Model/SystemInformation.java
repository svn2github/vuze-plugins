package Model;

/**
 * Created by Bruno Mendonça with IntelliJ IDEA.
 * User: brunol
 * Date: 20/06/11
 * Time: 17:14
 */
public final class SystemInformation {
    private String versionNumber;
    private String systemName;

    public String getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(String versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getSystemName() {
        return systemName;
    }

    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }
}
