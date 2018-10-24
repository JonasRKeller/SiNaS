package de.sinas.server;

import de.sinas.User;

import java.security.PublicKey;

import javax.crypto.SecretKey;

public class TempUser extends User {
	private SecretKey aesKey;
	private PublicKey rsaKey;

	public TempUser(String pClientIP, int pClientPort) {
		super(pClientIP, pClientPort, null, null);
	}

	/**
	 * @return the aesKey
	 */
	public SecretKey getAesKey() {
		return aesKey;
	}

	/**
	 * @param aesKey the aesKey to set
	 */
	public void setAesKey(SecretKey aesKey) {
		this.aesKey = aesKey;
	}

	/**
	 * @return the rsaKey
	 */
	public PublicKey getRsaKey() {
		return rsaKey;
	}

	/**
	 * @param rsaKey the rsaKey to set
	 */
	public void setRsaKey(PublicKey rsaKey) {
		this.rsaKey = rsaKey;
	}

	@Override
	public String getUsername() {
		throw new IllegalStateException("a temporary user has no username!");
	}

	@Override
	public String getPasswordHash() {
		throw new IllegalStateException("a temporary user has no password hash!");
	}
}