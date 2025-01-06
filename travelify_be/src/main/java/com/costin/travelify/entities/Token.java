package com.costin.travelify.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tokens")
@Getter
@Setter
@NoArgsConstructor
public class Token {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String token;

    private boolean expired;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public Token(String token, boolean expired, User user) {
        this.token = token;
        this.expired = expired;
        this.user = user;
    }

}
