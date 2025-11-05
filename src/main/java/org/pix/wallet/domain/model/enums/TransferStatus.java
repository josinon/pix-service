package org.pix.wallet.domain.model.enums;

/** Transfer statuses. Allowed transitions: PENDING→CONFIRMED, PENDING→REJECTED (or idempotent). Terminal after CONFIRMED/REJECTED. */
public enum TransferStatus {
	PENDING,
	CONFIRMED,
	REJECTED;

	/**
	 * Determines if this status can transition to target status following business rules.
	 */
	public boolean canTransitionTo(TransferStatus target) {
		if (this == target) return true; // idempotent no-op
		if (this == PENDING && (target == CONFIRMED || target == REJECTED)) return true;
		return false;
	}
}

