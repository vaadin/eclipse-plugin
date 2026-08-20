package com.vaadin.plugin.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import com.vaadin.plugin.hotswap.JbrSelector;
import com.vaadin.plugin.hotswap.JbrSelector.JbrCandidate;

public class JbrSelectorTest {

	private static JbrCandidate jbr(int major, String fullVersion) {
		return new JbrCandidate(null, major, true, false, fullVersion);
	}

	private static JbrCandidate brokenJbr(int major, String fullVersion) {
		return new JbrCandidate(null, major, true, true, fullVersion);
	}

	private static JbrCandidate jdk(int major, String fullVersion) {
		return new JbrCandidate(null, major, false, false, fullVersion);
	}

	@Test
	public void picksJbr25WhenProjectRequires25() {
		List<JbrCandidate> candidates = Arrays.asList(jbr(21, "21.0.4"), jbr(25, "25.0.2"));

		Optional<JbrCandidate> result = JbrSelector.select(candidates, 25);

		assertTrue(result.isPresent());
		assertEquals(25, result.get().majorVersion());
	}

	@Test
	public void returnsEmptyWhenOnlyOlderJbrAvailable() {
		List<JbrCandidate> candidates = List.of(jbr(21, "21.0.4"));

		Optional<JbrCandidate> result = JbrSelector.select(candidates, 25);

		assertFalse(result.isPresent());
	}

	@Test
	public void prefersExactMajorMatchOverHigher() {
		List<JbrCandidate> candidates = Arrays.asList(jbr(21, "21.0.4"), jbr(25, "25.0.2"));

		Optional<JbrCandidate> result = JbrSelector.select(candidates, 21);

		assertTrue(result.isPresent());
		assertEquals(21, result.get().majorVersion());
	}

	@Test
	public void prefersJbrOverNonJbrEvenWhenNonJbrIsExactMatch() {
		List<JbrCandidate> candidates = Arrays.asList(jdk(17, "17.0.10"), jdk(21, "21.0.5"), jbr(25, "25.0.2"));

		Optional<JbrCandidate> result = JbrSelector.select(candidates, 21);

		assertTrue(result.isPresent());
		assertTrue(result.get().isJbr());
		assertEquals(25, result.get().majorVersion());
	}

	@Test
	public void picksLatestPatchWithinSameMajor() {
		List<JbrCandidate> candidates = Arrays.asList(jbr(21, "21.0.1"), jbr(21, "21.0.5"), jbr(21, "21.0.3"));

		Optional<JbrCandidate> result = JbrSelector.select(candidates, 17);

		assertTrue(result.isPresent());
		assertEquals("21.0.5", result.get().fullVersion());
	}

	@Test
	public void filtersOutBrokenJbrs() {
		List<JbrCandidate> candidates = Arrays.asList(brokenJbr(21, "21.0.4+13-b509.17"), jbr(25, "25.0.2"));

		Optional<JbrCandidate> result = JbrSelector.select(candidates, 17);

		assertTrue(result.isPresent());
		assertEquals(25, result.get().majorVersion());
	}

	@Test
	public void fallsBackToNonJbrJdkWhenNoCompatibleJbr() {
		List<JbrCandidate> candidates = Arrays.asList(jbr(17, "17.0.5"), jdk(21, "21.0.5"));

		Optional<JbrCandidate> result = JbrSelector.select(candidates, 21);

		assertTrue(result.isPresent());
		assertFalse(result.get().isJbr());
		assertEquals(21, result.get().majorVersion());
	}

	@Test
	public void handlesJava8RequiredAgainstHigherJbr() {
		List<JbrCandidate> candidates = List.of(jbr(21, "21.0.4"));

		Optional<JbrCandidate> result = JbrSelector.select(candidates, 8);

		assertTrue(result.isPresent());
		assertEquals(21, result.get().majorVersion());
	}

	@Test
	public void emptyCandidatesReturnsEmpty() {
		Optional<JbrCandidate> result = JbrSelector.select(List.of(), 21);

		assertFalse(result.isPresent());
	}

	@Test
	public void parseMajorHandlesLegacyOneDotEight() {
		assertEquals(8, JbrSelector.parseMajor("1.8"));
		assertEquals(8, JbrSelector.parseMajor("1.8.0_362"));
	}

	@Test
	public void parseMajorHandlesModernVersions() {
		assertEquals(21, JbrSelector.parseMajor("21"));
		assertEquals(21, JbrSelector.parseMajor("21.0.4"));
		assertEquals(25, JbrSelector.parseMajor("25.0.2"));
		assertEquals(25, JbrSelector.parseMajor("25-ea"));
	}

	@Test
	public void parseMajorReturnsZeroForBlankInput() {
		assertEquals(0, JbrSelector.parseMajor(null));
		assertEquals(0, JbrSelector.parseMajor(""));
		assertEquals(0, JbrSelector.parseMajor("nonsense"));
	}

	@Test
	public void compareVersionsHandlesMultiDigitPatch() {
		assertTrue(JbrSelector.compareVersions("21.0.10", "21.0.4") > 0);
		assertTrue(JbrSelector.compareVersions("21.0.4", "21.0.10") < 0);
		assertEquals(0, JbrSelector.compareVersions("21.0.4", "21.0.4"));
	}
}
