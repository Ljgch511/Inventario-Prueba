package com.globalr.inventory.modules.asset.domain.exception;

/**
 * Excepción lanzada cuando una operación o dato viola las reglas de negocio de un activo.
 */
public class InvalidAssetException extends RuntimeException {

    public InvalidAssetException(String message) {
        super(message);
    }
}
