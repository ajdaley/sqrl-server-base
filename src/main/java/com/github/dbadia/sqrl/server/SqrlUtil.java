package com.github.dbadia.sqrl.server;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Arrays;
import java.util.Base64;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;

/**
 * Various utility methods used by the rest of the SQRL code, including the SQRL base64url derivitive
 * 
 * @author Dave Badia
 *
 */
public class SqrlUtil {
	private static final Logger logger = LoggerFactory.getLogger(SqrlUtil.class);

	private SqrlUtil() {
		// Util class
	}

	/**
	 * Performs the SQRL required base64URL encoding
	 * 
	 * @param bytes
	 *            the data to be encoded
	 * @return the encoded string
	 */
	public static String sqrlBase64UrlEncode(final byte[] bytes) {
		try {
			String encoded = new String(Base64.getUrlEncoder().encode(bytes), SqrlConstants.UTF8);
			while (encoded.endsWith("=")) {
				encoded = encoded.substring(0, encoded.length() - 1);
			}
			return encoded;
		} catch (final UnsupportedEncodingException e) {
			throw new IllegalStateException("UnsupportedEncodingException during base64 encode", e);
		}
	}

	/**
	 * Performs the SQRL required base64URL encoding
	 * 
	 * @param toEncode
	 *            the string to be encoded
	 * @return the encoded string
	 */
	public static String sqrlBase64UrlEncode(final String toEncode) {
		try {
			return sqrlBase64UrlEncode(toEncode.getBytes(SqrlConstants.UTF8));
		} catch (final UnsupportedEncodingException e) {
			throw new IllegalStateException("UnsupportedEncodingException ", e);
		}
	}

	/**
	 * Performs the SQRL required base64URL decoding
	 * 
	 * @param toDecodeParam
	 *            the data to be decoded
	 * @return the decoded byte array
	 * @throws SqrlException
	 *             if an error occurs
	 */
	public static byte[] base64UrlDecode(final String toDecodeParam) throws SqrlException {
		try {
			return Base64.getUrlDecoder().decode(toDecodeParam.getBytes());
		} catch (final IllegalArgumentException e) {
			throw new SqrlException("Error base64 decoding: " + toDecodeParam, e);
		}
	}

	/**
	 * Performs the SQRL required base64URL decoding
	 * 
	 * @param toDecodeParam
	 *            the data to be decoded
	 * @return the decoded data as a string using the UTF-8 character set
	 * @throws SqrlException
	 *             if UTF8 is not supported
	 */
	public static String base64UrlDecodeToString(final String toDecode) throws SqrlException {
		try {
			return new String(base64UrlDecode(toDecode), SqrlConstants.UTF8);
		} catch (final UnsupportedEncodingException e) {
			// This should never happen as the java specification requires that all JVMs support UTF8
			throw new SqrlException("UnsupportedEncodingException for " + SqrlConstants.UTF8, e);
		}
	}

	public static String base64UrlDecodeToStringOrErrorMessage(final String toDecode) {
		try {
			return base64UrlDecodeToString(toDecode);
		} catch (final Exception e) {
			logger.error("Error during url decode, returning error string for " + toDecode, e);
			return "<error during base64url decode>";
		}
	}

	/**
	 * Internal use only. Verifies the ED25519 signature
	 * 
	 * @param signatureFromMessage
	 *            the signature data
	 * @param messageBytes
	 *            the message that was signed
	 * @param publicKeyBytes
	 *            the public key to be used for verification
	 * @return true if verification was successful
	 * @throws SqrlException
	 *             if an error occurs during ED25519 operations
	 */
	public static boolean verifyED25519(final byte[] signatureFromMessage, final byte[] messageBytes,
			final byte[] publicKeyBytes) throws SqrlException {
		try {
			final Signature signature = new EdDSAEngine(MessageDigest.getInstance("SHA-512"));
			final EdDSAParameterSpec edDsaSpec = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.CURVE_ED25519_SHA512);

			final PublicKey publicKey = new EdDSAPublicKey(new EdDSAPublicKeySpec(publicKeyBytes, edDsaSpec));
			signature.initVerify(publicKey);

			signature.update(messageBytes);
			return signature.verify(signatureFromMessage);
		} catch (final GeneralSecurityException e) {
			throw new SqrlException("Got exception during EC signature verification", e);
		}
	}

	/**
	 * Provides the functionality of Apache commons StringUtils.isBlank() without bringing in the dependency
	 * 
	 * @param string
	 *            the string to check
	 * @return true if blank, false otherwise
	 */
	public static boolean isBlank(final String string) {
		return string == null || string.trim().length() == 0;
	}

	/**
	 * Provides the functionality of Apache commons StringUtils.isNotBlank() without bringing in the dependency
	 * 
	 * @param string
	 *            the string to check
	 * @return true if not blank, false otherwise
	 */
	public static boolean isNotBlank(final String string) {
		return !isBlank(string);
	}

	/**
	 * Internal use only.
	 * 
	 * @param ipAddressString
	 *            the ip address to parse
	 * @return the IP address
	 * @throws SqrlException
	 *             if an {@link UnknownHostException} occurs
	 */
	public static InetAddress ipStringToInetAddresss(final String ipAddressString) throws SqrlException {
		if (SqrlUtil.isBlank(ipAddressString)) {
			throw new SqrlException("ipAddressString was null or empty");
		}
		try {
			return InetAddress.getByName(ipAddressString);
		} catch (final UnknownHostException e) {
			throw new SqrlException("Got UnknownHostException for <" + ipAddressString + ">", e);
		}
	}

	/**
	 * Internal use only. Computes the SQRL server friendly name (SFN) from the servers URl. Typically used if a SFN is
	 * not specified in the config
	 * 
	 * @param request
	 * @return
	 */
	public static String computeSfnFromUrl(final HttpServletRequest request) {
		return request.getServerName();
	}

	/**
	 * Internal use only. Builds a string of name value pairs from the request
	 * 
	 * @param servletRequest
	 *            the request
	 * @return a string of the name value pairs that were in the request
	 */
	public static String buildRequestParamList(final HttpServletRequest servletRequest) {
		final Enumeration<String> params = servletRequest.getParameterNames();
		final StringBuilder buf = new StringBuilder();
		while (params.hasMoreElements()) {
			final String paramName = params.nextElement();
			buf.append(paramName).append("=").append(servletRequest.getParameter(paramName)).append("  ");
		}
		return buf.toString();
	}

	public static Cookie createCookie(final HttpServletRequest request, final HttpServletResponse response,
			final String name, final String value) {
		final Cookie cookie = new Cookie(name, value);
		cookie.setHttpOnly(true);
		if (SqrlConstants.SCHEME_HTTPS.equals(request.getScheme())) {
			cookie.setSecure(true);
		}
		cookie.setMaxAge(-1);
		return cookie;
	}

	public static String getCookieValue(final HttpServletRequest request, final String toFind) {
		if (request.getCookies() == null) {
			return null;
		}
		for (final Cookie cookie : request.getCookies()) {
			if (toFind.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		return null;
	}

	public static void deleteAllCookies(final HttpServletRequest request, final HttpServletResponse response) {
		for (final Cookie cookie : request.getCookies()) {
			cookie.setMaxAge(0);
			response.addCookie(cookie);
		}
	}

	public static void deleteCookies(final HttpServletRequest request, final HttpServletResponse response,
			final String... cookiesToDelete) {
		final List<String> cookieToDeleteList = Arrays.asList(cookiesToDelete);
		for (final Cookie cookie : request.getCookies()) {
			if (cookieToDeleteList.contains(cookie.getName())) {
				cookie.setMaxAge(0);
				response.addCookie(cookie);
			}
		}
	}
}
