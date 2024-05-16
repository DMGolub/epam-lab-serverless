package com.task11;

public class Table {

	private int id;
	private int number;
	private int places;
	private boolean isVip;
	private int minOrder;

	public Table() {
		// Empty
	}

	public Table(final int id, final int number, final int places, final boolean isVip, final int minOrder) {
		this.id = id;
		this.number = number;
		this.places = places;
		this.isVip = isVip;
		this.minOrder = minOrder;
	}

	public int getId() {
		return id;
	}

	public void setId(final int id) {
		this.id = id;
	}

	public int getNumber() {
		return number;
	}

	public void setNumber(final int number) {
		this.number = number;
	}

	public int getPlaces() {
		return places;
	}

	public void setPlaces(final int places) {
		this.places = places;
	}

	public boolean getIsVip() {
		return isVip;
	}

	public void setIsVip(final boolean vip) {
		isVip = vip;
	}

	public int getMinOrder() {
		return minOrder;
	}

	public void setMinOrder(final int minOrder) {
		this.minOrder = minOrder;
	}

	@Override
	public String toString() {
		return "Table{" +
			"id=" + id +
			", number=" + number +
			", places=" + places +
			", isVip=" + isVip +
			", minOrder=" + minOrder +
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

		final Table table = (Table) o;

		if (id != table.id) {
			return false;
		}
		if (number != table.number) {
			return false;
		}
		if (places != table.places) {
			return false;
		}
		if (isVip != table.isVip) {
			return false;
		}
		return minOrder == table.minOrder;
	}

	@Override
	public int hashCode() {
		int result = id;
		result = 31 * result + number;
		result = 31 * result + places;
		result = 31 * result + (isVip ? 1 : 0);
		result = 31 * result + minOrder;
		return result;
	}
}
