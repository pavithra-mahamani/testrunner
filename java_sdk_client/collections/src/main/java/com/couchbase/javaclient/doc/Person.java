package com.couchbase.javaclient.doc;

import com.github.javafaker.Faker;
import com.couchbase.client.java.json.JsonObject;

public class Person {
	String firstName;
	String lastName;
	String streetAddress;
	JsonObject jsonObject;

	public Person() {
		Faker faker = new Faker();
		setFirstName(faker.name().firstName());
		setLastName(faker.name().lastName());
		setStreetAddress(faker.address().streetAddress());
		setJsonObject(JsonObject.create());
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getStreetAddress() {
		return streetAddress;
	}

	public void setStreetAddress(String streetAddress) {
		this.streetAddress = streetAddress;

	}

	@Override
	public String toString() {
		return "{" + "\"firstName\":" + firstName + "," + "\"lastName\":" + lastName + "," + "\"streetAddress\":"
				+ streetAddress + "}";
	}

	public JsonObject getJsonObject() {
		return jsonObject;
	}

	public void setJsonObject(JsonObject jsonObject) {
		this.jsonObject = jsonObject;
		this.jsonObject.put("firstName", firstName);
		this.jsonObject.put("lastName", lastName);
		this.jsonObject.put("streetAddress", streetAddress);
	}
}
