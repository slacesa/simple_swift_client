package com.slacesa.simpleSwiftClient.resources;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enumerates the available values of policy_retrieval_state
 * 
 * @author SLC
 *
 */
public @JsonFormat
enum PolicyRetrievalStates {
	SEALED("sealed"),
	UNSEALING("unsealing"),
	UNSEALED("unsealed");

	private String state;

	private PolicyRetrievalStates(String state) {
		this.state = state;
	}

	@JsonValue
	public String getValue() {
		return this.state;
	}

	@JsonCreator
    public static PolicyRetrievalStates fromState(String value) {
		if(value == null) return null;
		for(PolicyRetrievalStates s : PolicyRetrievalStates.values())
			if(s.getValue().equals(value)) return s;
		throw new IllegalArgumentException("State \"" + value + "\" not allowed");
    }
}
