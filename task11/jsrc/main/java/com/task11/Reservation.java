package com.task11;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import static com.task11.Constants.HH_MM;
import static com.task11.Constants.YYYY_MM_DD;

public class Reservation {

	private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(HH_MM);
	private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(YYYY_MM_DD);

	private int tableNumber;
	private String clientName;
	private String phoneNumber;
	private LocalDate date;
	private LocalTime slotTimeStart;
	private LocalTime slotTimeEnd;

	public Reservation() {
		// Empty
	}

	public Reservation(
		final int tableNumber,
		final String clientName,
		final String phoneNumber,
		final String date,
		final String slotTimeStart,
		final String slotTimeEnd
	) {
		this.tableNumber = tableNumber;
		this.clientName = clientName;
		this.phoneNumber = phoneNumber;

		final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(YYYY_MM_DD);
		this.date = LocalDate.parse(date, dateFormatter);

		final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(HH_MM);
		this.slotTimeStart = LocalTime.parse(slotTimeStart, timeFormatter);
		this.slotTimeEnd = LocalTime.parse(slotTimeEnd, timeFormatter);
	}

	public int getTableNumber() {
		return tableNumber;
	}

	public void setTableNumber(final int tableNumber) {
		this.tableNumber = tableNumber;
	}

	public String getClientName() {
		return clientName;
	}

	public void setClientName(final String clientName) {
		this.clientName = clientName;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(final String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getDate() {
		return dateFormatter.format(date);
	}

	public void setDate(final String date) {
		this.date = LocalDate.parse(date, dateFormatter);
	}

	public String getSlotTimeStart() {
		return timeFormatter.format(slotTimeStart);
	}

	public void setSlotTimeStart(final String slotTimeStart) {
		this.slotTimeStart = LocalTime.parse(slotTimeStart, timeFormatter);
	}

	public String getSlotTimeEnd() {
		return timeFormatter.format(slotTimeEnd);
	}

	public void setSlotTimeEnd(final String slotTimeEnd) {
		this.slotTimeEnd = LocalTime.parse(slotTimeEnd, timeFormatter);
	}

	@Override
	public String toString() {
		return "Reservation{" +
			"tableNumber=" + tableNumber +
			", clientName='" + clientName + '\'' +
			", phoneNumber='" + phoneNumber + '\'' +
			", date=" + date +
			", slotTimeStart=" + slotTimeStart +
			", slotTimeEnd=" + slotTimeEnd +
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

		final Reservation that = (Reservation) o;

		if (tableNumber != that.tableNumber) {
			return false;
		}
		if (!Objects.equals(clientName, that.clientName)) {
			return false;
		}
		if (!Objects.equals(phoneNumber, that.phoneNumber)) {
			return false;
		}
		if (!Objects.equals(date, that.date)) {
			return false;
		}
		if (!Objects.equals(slotTimeStart, that.slotTimeStart)) {
			return false;
		}
		return Objects.equals(slotTimeEnd, that.slotTimeEnd);
	}

	@Override
	public int hashCode() {
		int result = tableNumber;
		result = 31 * result + (clientName != null ? clientName.hashCode() : 0);
		result = 31 * result + (phoneNumber != null ? phoneNumber.hashCode() : 0);
		result = 31 * result + (date != null ? date.hashCode() : 0);
		result = 31 * result + (slotTimeStart != null ? slotTimeStart.hashCode() : 0);
		result = 31 * result + (slotTimeEnd != null ? slotTimeEnd.hashCode() : 0);
		return result;
	}
}
