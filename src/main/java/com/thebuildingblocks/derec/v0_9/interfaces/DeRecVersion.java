package com.thebuildingblocks.derec.v0_9.interfaces;

/**
 * Represents an iteration of a value of a secret. A new Version is created when a {@link DeRecSecret} is updated.
 * It is distributed among helpers. If a sufficient number of helpers acknowledge receipt {@link #isProtected()}
 * returns true, representing that this version can be recovered.
 */
public interface DeRecVersion <S extends DeRecSecret<S, V, H, I>, V extends DeRecVersion<S, V, H, I>, H extends DeRecPairable, I extends DeRecId>{

    /**
     * The secret this version is a secret of
     */
    S getSecret();

    /**
     * The version number of this Version. Later versions have higher numbers.
     */
    long getVersionNumber();

    /**
     * The value of the secret at this version
     */
    byte[] getProtectedValue();

    /**
     * The version has been successfully distributed among helpers.
     */
    boolean isProtected();
}
