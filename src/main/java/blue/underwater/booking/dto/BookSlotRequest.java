package blue.underwater.booking.dto;

public class BookSlotRequest {
    private String name;
    private String email;
    private String lang = "es";

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getLang() { return lang != null ? lang : "es"; }
    public void setLang(String lang) { this.lang = lang; }
}
