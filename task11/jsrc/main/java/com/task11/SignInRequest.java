package com.task11;

import java.util.Objects;

public class SignInRequest {

	private String email;
	private String password;

	public SignInRequest() {
		// Empty
	}

	public SignInRequest(final String email, final String password) {
		this.email = email;
		this.password = password;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(final String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(final String password) {
		this.password = password;
	}

	@Override
	public String toString() {
		return "SignInRequest{" +
			"email='" + email + '\'' +
			", password='" + password + '\'' +
			'}';
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final SignInRequest that = (SignInRequest) o;

		if (!Objects.equals(email, that.email)) {
			return false;
		}
		return Objects.equals(password, that.password);
	}

	@Override
	public int hashCode() {
		int result = email != null ? email.hashCode() : 0;
		result = 31 * result + (password != null ? password.hashCode() : 0);
		return result;
	}
}
