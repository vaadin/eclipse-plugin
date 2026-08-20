package com.vaadin.plugin.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.debug.core.DebugPlugin;
import org.junit.Test;

import com.vaadin.plugin.hotswap.HotswapAgentManager;

/**
 * Verifies that the JVM argument string emitted for Hotswap launches survives Eclipse's whitespace-based
 * tokenizer. Regression test for issue #84: a missing space between {@code --add-opens} and the package list
 * caused tokens like {@code --add-opensjava.base/...=ALL-UNNAMED} which the JVM rejected.
 */
public class HotswapJvmArgsTest {

	@Test
	public void noTokenContainsRegressionSubstring() throws IOException {
		String args = HotswapAgentManager.getInstance().getHotswapJvmArgsString(true);

		String[] tokens = DebugPlugin.parseArguments(args);

		assertTrue("Expected at least one token", tokens.length > 0);
		for (String token : tokens) {
			assertFalse("Token must not contain '--add-opensjava' (mashed --add-opens bug): " + token,
					token.contains("--add-opensjava"));
		}
	}

	@Test
	public void everyTokenStartsWithExpectedPrefix() throws IOException {
		String args = HotswapAgentManager.getInstance().getHotswapJvmArgsString(true);

		String[] tokens = DebugPlugin.parseArguments(args);

		for (String token : tokens) {
			boolean ok = token.startsWith("-javaagent:") || token.startsWith("-XX:")
					|| token.startsWith("--add-opens=") || token.startsWith("-D");
			assertTrue("Unexpected token shape: '" + token + "'", ok);
		}
	}

	@Test
	public void withoutJbrFlagsOmitsExactlyTheThreeJbrOnlyFlags() throws IOException {
		HotswapAgentManager mgr = HotswapAgentManager.getInstance();

		Set<String> withJbr = new HashSet<>(Arrays.asList(DebugPlugin.parseArguments(mgr.getHotswapJvmArgsString(true))));
		Set<String> withoutJbr = new HashSet<>(
				Arrays.asList(DebugPlugin.parseArguments(mgr.getHotswapJvmArgsString(false))));

		Set<String> removed = new HashSet<>(withJbr);
		removed.removeAll(withoutJbr);

		Set<String> expected = Set.of("-XX:+AllowEnhancedClassRedefinition", "-XX:+ClassUnloading",
				"-XX:HotswapAgent=external");
		assertEquals("withJbrFlags=false should remove exactly the three JBR-only flags", expected, removed);

		Set<String> added = new HashSet<>(withoutJbr);
		added.removeAll(withJbr);
		assertTrue("withJbrFlags=false should not add tokens", added.isEmpty());
	}

	@Test
	public void includesAllExpectedAddOpensTokens() throws IOException {
		String args = HotswapAgentManager.getInstance().getHotswapJvmArgsString(true);

		String[] tokens = DebugPlugin.parseArguments(args);
		Set<String> tokenSet = new HashSet<>(Arrays.asList(tokens));

		// Sample of the most load-bearing opens — full list is enforced by the regression
		// substring assertion plus the prefix assertion.
		assertTrue("Missing --add-opens=java.base/java.lang=ALL-UNNAMED",
				tokenSet.contains("--add-opens=java.base/java.lang=ALL-UNNAMED"));
		assertTrue("Missing --add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
				tokenSet.contains("--add-opens=java.base/java.lang.reflect=ALL-UNNAMED"));
		assertTrue("Missing --add-opens=jdk.management/com.sun.management.internal=ALL-UNNAMED",
				tokenSet.contains("--add-opens=jdk.management/com.sun.management.internal=ALL-UNNAMED"));
	}

	@Test
	public void javaagentSurvivesQuotedPath() {
		// Round-trip a -javaagent token whose path contains a space, exactly as
		// HotswapAgentManager emits it. DebugPlugin.parseArguments must return a single token.
		String emitted = "-javaagent:\"C:\\Program Files\\hotswap\\hotswap-agent.jar\" -Dfoo=bar";

		String[] tokens = DebugPlugin.parseArguments(emitted);

		assertEquals(2, tokens.length);
		assertEquals("-javaagent:C:\\Program Files\\hotswap\\hotswap-agent.jar", tokens[0]);
		assertEquals("-Dfoo=bar", tokens[1]);
	}
}
