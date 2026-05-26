package ru.demo.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Notification {

    private Long id;

    private LocalDateTime triggerAt;

    private String message;

    private boolean active;

    public Notification() {
    }

    public Notification(Long id, LocalDateTime triggerAt, String message, boolean active) {
        this.id = id;
        this.triggerAt = triggerAt;
        this.message = message;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getTriggerAt() {
        return triggerAt;
    }

    public void setTriggerAt(LocalDateTime triggerAt) {
        this.triggerAt = triggerAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "Notification{" +
                "id=" + id +
                ", triggerAt=" + triggerAt +
                ", message='" + message + '\'' +
                ", active=" + active +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Notification that)) {
            return false;
        }
        return active == that.active
                && Objects.equals(id, that.id)
                && Objects.equals(triggerAt, that.triggerAt)
                && Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, triggerAt, message, active);
    }
}
