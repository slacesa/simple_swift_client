package io.github.slacesa.simpleSwiftClient.resources;

import org.joda.time.DateTime;

/**
 * A File descriptor, contains the information retrieved by the Swift Server
 * @author SLC
 *
 */
public class SwiftFile {
	
	private String
		hash,
		name,
		content_type; 

	private Integer
		bytes,
		policy_retrieval_delay;

	private DateTime last_modified;
	
	private PolicyRetrievalStates policy_retrieval_state;

	public SwiftFile() {}

	/**
	 * The MD5 hash of the file content
	 * @return the hash value
	 */
	public String getHash() {
		return hash;
	}

	public void setHash(String hash) {
		this.hash = hash;
	}

	/**
	 * The filename
	 * @return the filename
	 */
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * When the file was last modified, compatible with DateTime format
	 * @return the last edit
	 */
	public String getLast_modified() {
		return last_modified.toString();
	}

	public void setLast_modified(DateTime last_modified) {
		this.last_modified = last_modified;
	}

	public void setLast_modified(String last_modified) {
		this.last_modified = DateTime.parse(last_modified);
	}

	/**
	 * The file content type
	 * @return
	 */
	public String getContent_type() {
		return content_type;
	}

	public void setContent_type(String content_type) {
		this.content_type = content_type;
	}

	/**
	 * The retrieval state
	 * @see io.github.slacesa.simpleSwiftClient.resources.PolicyRetrievalStates
	 * @return the state, can be absent (null)
	 */
	public String getPolicy_retrieval_state() {
		return policy_retrieval_state == null? null : policy_retrieval_state.getValue();
	}

	public void setPolicy_retrieval_state(String policy_retrieval_state) {
		this.policy_retrieval_state = PolicyRetrievalStates.fromState(policy_retrieval_state);
	}

	/**
	 * The file length in bytes
	 * @return the file length
	 */
	public Integer getBytes() {
		return bytes;
	}

	public void setBytes(Integer bytes) {
		this.bytes = bytes;
	}

	/**
	 * The retrieval delay in seconds
	 * @return the delay, can be absent (null)
	 */
	public Integer getPolicy_retrieval_delay() {
		return policy_retrieval_delay;
	}

	public void setPolicy_retrieval_delay(Integer policy_retrieval_delay) {
		this.policy_retrieval_delay = policy_retrieval_delay;
	}
}
