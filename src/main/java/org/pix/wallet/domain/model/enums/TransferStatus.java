package org.pix.wallet.domain.model.enums;

/**
 * Transfer lifecycle statuses.
 * Only transitions allowed:
 *   PENDING -> CONFIRMED
 *   PENDING -> REJECTED
 * Self (idempotent) transitions (e.g. PENDING -> PENDING) are tolerated.
 * After CONFIRMED or REJECTED the transfer is terminal.
 */
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

