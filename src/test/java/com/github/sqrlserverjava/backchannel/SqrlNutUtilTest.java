package com.github.sqrlserverjava.backchannel;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.time.LocalDateTime;

import org.junit.Before;
import org.junit.Test;

import com.github.sqrlserverjava.SqrlConfig;
import com.github.sqrlserverjava.SqrlPersistence;
import com.github.sqrlserverjava.TestCaseUtil;
import com.github.sqrlserverjava.exception.SqrlException;

import junitx.framework.ArrayAssert;
import junitx.framework.ObjectAssert;

public class SqrlNutUtilTest {
	private SqrlConfig		config		= TestCaseUtil.buildTestSqrlConfig();
	private SqrlPersistence	persistence	= null;

	@Before
	public void setup() throws Exception {
		config = TestCaseUtil.buildTestSqrlConfig();
		persistence = TestCaseUtil.createEmptySqrlPersistence();
	}

	@Test
	public void testInetAddressToInt_ipv4_https() throws Throwable {
		final URI url = new URI("https://davetest.com/sqrl");
		final InetAddress address = InetAddress.getByName("69.50.232.54");
		assertTrue(address instanceof Inet4Address);
		final int actual = SqrlNutTokenUtil.inetAddressToInt(url, address, config);
		assertEquals(1160964150, actual);
	}

	@Test
	public void testInetAddressToInt_ipv6_https() throws Throwable {
		final URI url = new URI("https://davetest.com/sqrl");
		final InetAddress address = InetAddress.getByName("2607:f258:102:3::2");
		assertTrue(address instanceof Inet6Address);
		final int actual = SqrlNutTokenUtil.inetAddressToInt(url, address, config);
		assertEquals(-461733409, actual);
	}

	@Test
	public void testInetAddressToInt_ipv6_sqrl() throws Throwable {
		final URI url = new URI("sqrl://davetest.com/sqrl");
		final InetAddress address = InetAddress.getByName("2607:f258:102:3::2");
		assertTrue(address instanceof Inet6Address);
		final int actual = SqrlNutTokenUtil.inetAddressToInt(url, address, config);
		assertEquals(-461733409, actual);
	}

	@Test
	public void testValidateInetAddress_ipv4_pass() throws Throwable {
		final InetAddress address = InetAddress.getByName("69.50.232.54");
		final boolean actual = !SqrlNutTokenUtil.validateInetAddress(address, 1160964150, config).isPresent();
		assertTrue(actual);
	}

	@Test
	public void testValidateInetAddress_ipv4_fail_1() throws Throwable {
		final InetAddress address = InetAddress.getByName("198.105.254.130");
		final boolean actual = !SqrlNutTokenUtil.validateInetAddress(address, 1160964150, config).isPresent();
		assertFalse(actual);
	}

	public void testValidateInetAddress_ipv4_fail_2() throws Throwable {
		final InetAddress address = InetAddress.getByName("69.50.232.54");
		final boolean actual = !SqrlNutTokenUtil.validateInetAddress(address, 1511609640, config).isPresent();
		assertFalse(actual);
	}

	@Test
	public void testValidateInetAddress_ipv6_pass() throws Throwable {
		final InetAddress address = InetAddress.getByName("2607:f258:102:3::2");
		final boolean actual = !SqrlNutTokenUtil.validateInetAddress(address, -461733409, config).isPresent();
		assertTrue(actual);
	}

	@Test
	public void testValidateInetAddress_ipv6_fail_1() throws Throwable {
		final InetAddress address = InetAddress.getByName("2001:0db8:85a3:0000:0000:8a2e:0370:7334");
		final boolean actual = !SqrlNutTokenUtil.validateInetAddress(address, 1160964150, config).isPresent();
		assertFalse(actual);
	}

	public void testValidateInetAddress_ipv6_fail_2() throws Throwable {
		final InetAddress address = InetAddress.getByName("2607:f258:102:3::2");
		final boolean actual = !SqrlNutTokenUtil.validateInetAddress(address, 1511609640, config).isPresent();
		assertFalse(actual);
	}

	@Test
	public void testPack() throws Throwable {
		final byte[] bytes = { 27, -78, -123, 54 };
		final int actual = SqrlNutTokenUtil.pack(bytes);
		assertEquals(464684342, actual);
	}

	@Test
	public void testUnpack() throws Throwable {
		final byte[] actual = SqrlNutTokenUtil.unpack(464684342);
		ArrayAssert.assertEquals(new byte[] { 27, -78, -123, 54 }, actual);
	}

	@Test
	public void testInetAddressV4ToInt_https() throws Exception {
		final InetAddress requesterIpAddress = InetAddress.getByName("127.0.0.1");
		final URI uri = new URI("https://grc.com");
		final int inetInt = SqrlNutTokenUtil.inetAddressToInt(uri, requesterIpAddress, config);
		assertEquals(2130706433, inetInt);
	}

	/* ************ Nut expiry tests *********************/
	@Test
	public void testNutExpiry() throws SqrlException {
		final int nutValidityInSeconds = 1000;
		config.setNutValidityInSeconds(nutValidityInSeconds);
		final SqrlNutToken nut = TestCaseUtil.buildValidSqrlNut(config);
		final long nutIssuedTime = nut.getIssuedTimestampMillis();
		final long expiresAt = SqrlNutTokenUtil.computeNutExpiresAt(nut, config);
		assertTrue(expiresAt > nutIssuedTime);
		assertEquals(nutValidityInSeconds * 1000, expiresAt - nutIssuedTime);
	}

	@Test
	public void testNutExpiry2() throws SqrlException {
		final int nutValidityInSeconds = 180;
		config.setNutValidityInSeconds(nutValidityInSeconds);
		final SqrlNutToken nut = TestCaseUtil.buildValidSqrlNut(config);
		final long nutIssuedTime = nut.getIssuedTimestampMillis();
		final long expiresAt = SqrlNutTokenUtil.computeNutExpiresAt(nut, config);
		assertTrue(expiresAt > nutIssuedTime);
		assertEquals(nutValidityInSeconds * 1000, expiresAt - nutIssuedTime);
	}

	/**
	 * timetsamp is unsigned int with second precision which means that the timestamp will go up to
	 * 2106-02-07T02:28:15-0400
	 *
	 * @throws SqrlException
	 */
	@Test
	public void testComputeNutExpiresInJan2016() throws SqrlException {
		final int nutValidityInSeconds = 1000;
		config.setNutValidityInSeconds(nutValidityInSeconds);
		final LocalDateTime tokenIssuedAt = LocalDateTime.parse("2016-01-03T10:15:30");
		final SqrlNutToken nut = TestCaseUtil.buildValidSqrlNut(config, tokenIssuedAt);
		final long nutIssuedTime = nut.getIssuedTimestampMillis();
		final long expiresAt = SqrlNutTokenUtil.computeNutExpiresAt(nut, config);
		assertTrue(expiresAt > nutIssuedTime);
		assertEquals(nutValidityInSeconds * 1000, expiresAt - nutIssuedTime);
		assertEquals(1452816000L, expiresAt);
	}

	@Test
	public void testValidateNutTimestamp_ExpiredInJan2016() throws SqrlException {
		final int nutValidityInSeconds = 1000;
		config.setNutValidityInSeconds(nutValidityInSeconds);
		final LocalDateTime tokenIssuedAt = LocalDateTime.parse("2016-01-03T10:15:30");
		final SqrlNutToken nutToken = TestCaseUtil.buildValidSqrlNut(config, tokenIssuedAt);
		try {
			SqrlNutTokenUtil.validateNut("123", nutToken, config, persistence);
			fail("Exception expected");
		} catch (final Exception e) {
			ObjectAssert.assertInstanceOf(SqrlException.class, e);
		}
	}
}
