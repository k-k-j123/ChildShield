package com.kaushik.ChildShield.config;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    private ResponseEntity<Resource> servePage(String filename) {
        Resource resource = new ClassPathResource("templates/" + filename);
        if (resource.exists()) {
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(resource);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping(value = {"/", "/index", "/index.html", "/login", "/login.html"})
    public ResponseEntity<Resource> index() {
        return servePage("index.html");
    }

    @GetMapping(value = {"/register", "/register.html"})
    public ResponseEntity<Resource> registerHtml() {
        return servePage("register.html");
    }

    @GetMapping(value = {"/admin-dashboard", "/admin-dashboard.html"})
    public ResponseEntity<Resource> adminDashboard() {
        return servePage("admin-dashboard.html");
    }

    @GetMapping(value = {"/worker-dashboard", "/worker-dashboard.html", "/health-worker-dashboard", "/health-worker-dashboard.html"})
    public ResponseEntity<Resource> workerDashboard() {
        return servePage("worker-dashboard.html");
    }

    @GetMapping(value = {"/mother-dashboard", "/mother-dashboard.html", "/user-dashboard", "/user-dashboard.html"})
    public ResponseEntity<Resource> motherDashboard() {
        return servePage("mother-dashboard.html");
    }

    @GetMapping(value = {"/beneficiaries", "/beneficiaries.html"})
    public ResponseEntity<Resource> beneficiaries() {
        return servePage("beneficiaries.html");
    }

    @GetMapping(value = {"/workers", "/workers.html", "/healthcare-workers.html"}, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> workers() {
        return servePage("workers.html");
    }

    @GetMapping(value = {"/vaccinations.html", "/vaccination-schedule", "/vaccination-schedule.html"})
    public ResponseEntity<Resource> vaccinations() {
        return servePage("vaccinations.html");
    }

    @GetMapping(value = {"/maternal-visits.html", "/visit-records", "/visit-records.html"})
    public ResponseEntity<Resource> maternalVisits() {
        return servePage("maternal-visits.html");
    }

    @GetMapping(value = {"/villages.html"})
    public ResponseEntity<Resource> villages() {
        return servePage("villages.html");
    }

    @GetMapping(value = {"/reports.html"})
    public ResponseEntity<Resource> reportsHtml() {
        return servePage("reports.html");
    }

    @GetMapping(value = {"/profile.html"})
    public ResponseEntity<Resource> profileHtml() {
        return servePage("profile.html");
    }

    @GetMapping(value = {"/notifications.html"})
    public ResponseEntity<Resource> notificationsHtml() {
        return servePage("notifications.html");
    }
}
