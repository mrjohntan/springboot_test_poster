package com.example.poster.dto;

public class RequestData {
    private String url;
    private String username;
    private String password;
    private String user;
    private String workspace;
    private String env;

    // Constructors
    public RequestData() {}

    public RequestData(String url, String username, String password, String user, String workspace, String env) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.user = user;
        this.workspace = workspace;
        this.env = env;
    }

    // Getters and Setters
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    public String getWorkspace() { return workspace; }
    public void setWorkspace(String workspace) { this.workspace = workspace; }

    public String getEnv() { return env; }
    public void setEnv(String env) { this.env = env; }
}
