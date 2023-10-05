package com.thebuildingblocks.derec.v0_9.interfaces;

import java.util.List;
import java.util.UUID;

/**
 * A factory for and container of Secrets in this API
 */
public interface DeRecSharer <S extends DeRecSecret<S, V, H, I>, V extends DeRecVersion<S, V, H, I>, H extends DeRecPairable, I extends DeRecId>{

    /**
     * Create a new secret and auto-allocate its ID
     *
     * @param description    a human readable description
     * @param bytesToProtect the content of the secret
     * @param helperIds      the ids of helpers for this secret
     * @return a secret
     */
    S newSecret(String description, byte[] bytesToProtect, List<DeRecId> helperIds);

    /**
     * Create a new secret
     *
     * @param secretId       a UUID to uniquely identify this secret
     * @param description    a human readable description
     * @param bytesToProtect the content of the secret
     * @param helperIds      the ids of helpers for this secret
     * @return a secret
     */
    S newSecret(UUID secretId, String description, byte[] bytesToProtect, List<DeRecId> helperIds);

    /**
     * Get the secret with this UUID, return null if none with this ID
     *
     * @param secretId a secret ID
     * @return a secret or null
     */
    S getSecret(UUID secretId);

    /**
     * Get a list of all secrets known to this sharer
     *
     * @return a list
     */
    List<S> getSecrets();
}
