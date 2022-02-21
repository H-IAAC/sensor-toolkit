package br.org.eldorado.hiaac.firebase;

public interface FirebaseListener {

    public void onProgress(String message);
    public void onCompleted(String message);
}
