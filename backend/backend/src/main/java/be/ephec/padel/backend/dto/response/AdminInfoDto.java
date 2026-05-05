package be.ephec.padel.backend.dto.response;

public class AdminInfoDto {

    private final String status;
    private final String adminType;
    private final Long siteId;
    private final String siteNom;

    public AdminInfoDto(String status, String adminType, Long siteId, String siteNom) {
        this.status = status;
        this.adminType = adminType;
        this.siteId = siteId;
        this.siteNom = siteNom;
    }

    public String getStatus() {
        return status;
    }

    public String getAdminType() {
        return adminType;
    }

    public Long getSiteId() {
        return siteId;
    }

    public String getSiteNom() {
        return siteNom;
    }
}
