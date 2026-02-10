package com.example.geschenkplaner.data;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

// Zentrale Hilfsklasse für alle Firestore-Pfade
public final class FirestorePaths {

    // Verhindert Instanzen dieser Klasse (nur statische Methoden)
    private FirestorePaths() {}

    // users/{uid} → Basis-Dokument eines Users
    public static DocumentReference user(String uid) {
        return FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid);
    }

    // users/{uid}/persons → alle Personen eines Users
    public static CollectionReference persons(String uid) {
        return user(uid).collection("persons");
    }

    // users/{uid}/persons/{personId} → eine bestimmte Person
    public static DocumentReference person(String uid, String personId) {
        return persons(uid).document(personId);
    }

    // users/{uid}/persons/{personId}/gifts → Geschenke einer Person
    public static CollectionReference gifts(String uid, String personId) {
        return person(uid, personId).collection("gifts");
    }

    // users/{uid}/persons/{personId}/gifts/{giftId} → einzelnes Geschenk
    public static DocumentReference gift(String uid, String personId, String giftId) {
        return gifts(uid, personId).document(giftId);
    }

    // users/{uid}/events → alle Events eines Users
    public static CollectionReference events(String uid) {
        return user(uid).collection("events");
    }

    // users/{uid}/events/{eventId} → einzelnes Event
    public static DocumentReference event(String uid, String eventId) {
        return events(uid).document(eventId);
    }
}
