package com.example.geschenkplaner.data;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public final class FirestorePaths {

    private FirestorePaths() {}

    public static DocumentReference user(String uid) {
        return FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid);
    }

    public static CollectionReference persons(String uid) {
        return user(uid).collection("persons");
    }

    public static DocumentReference person(String uid, String personId) {
        return persons(uid).document(personId);
    }

    public static CollectionReference gifts(String uid, String personId) {
        return person(uid, personId).collection("gifts");
    }

    public static DocumentReference gift(String uid, String personId, String giftId) {
        return gifts(uid, personId).document(giftId);
    }

    public static CollectionReference events(String uid) {
        return user(uid).collection("events");
    }

    public static DocumentReference event(String uid, String eventId) {
        return events(uid).document(eventId);
    }
}
