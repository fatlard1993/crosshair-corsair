package justfatlard.crosshair_corsair.crosshair;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * When a held item's crosshair shows.
 *
 * <p>Four answers rather than two, because "show it" is not a yes/no question for something in the
 * hand. An unknown word is {@link #ALWAYS}.
 */
public enum Policy {
	ALWAYS,
	/** Something - anything - is under the crosshair. */
	TARGETING,
	/** This item would do something to what is under the crosshair. */
	INTERACTABLE,
	NEVER;

	/**
	 * The two facts a policy can turn on.
	 *
	 * <p>Named, so a call site that passes the same expression for both is visible on the page:
	 * {@code interactable} must always be the narrower of the two, or the menu label promises a
	 * tightening and delivers a loosening.
	 */
	public record Facts(boolean targeting, boolean interactable) {}

	public boolean shows(Facts facts) {
		return switch (this) {
			case NEVER -> false;
			case TARGETING -> facts.targeting();
			case INTERACTABLE -> facts.interactable();
			case ALWAYS -> true;
		};
	}

	/** Read on every frame the crosshair draws, so an unknown word costs a lookup and not a throw. */
	private static final Map<String, Policy> BY_NAME = Arrays.stream(values())
		.collect(Collectors.toUnmodifiableMap(Enum::name, Function.identity()));

	public static Policy named(String name) {
		if (name == null) return ALWAYS;
		return BY_NAME.getOrDefault(name.trim().toUpperCase(Locale.ROOT), ALWAYS);
	}

	public String configName() {
		return name().toLowerCase(Locale.ROOT);
	}
}
