/*
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.billing.api.search;

import java.util.List;
import java.util.Calendar;
import java.util.Date;
import org.hibernate.Criteria;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.hibernate.HibernatePatientDAO;
import org.openmrs.module.billing.api.base.entity.search.BaseDataTemplateSearch;
import org.openmrs.module.billing.api.model.Bill;
import org.openmrs.module.billing.api.model.BillLineItem;
import org.openmrs.module.billing.api.model.BillStatus;

/**
 * A search template class for the {@link Bill} model.
 */
public class BillSearch extends BaseDataTemplateSearch<Bill> {
	
	private Boolean includeVoidedLineItems;
	
	private List<BillStatus> statuses;
	
	private String patientName;
	
	private Date fromDate;
	
	private Date toDate;
	
	private List<Location> locations;
	
	private Provider provider;
	
	public BillSearch() {
		this(new Bill(), false);
	}
	
	public BillSearch(Bill template) {
		this(template, false);
	}
	
	public BillSearch(Bill template, Boolean includeRetired) {
		super(template, includeRetired);
		this.includeVoidedLineItems = false;
	}
	
	/**
	 * Sets whether voided line items should be included in the results.
	 * 
	 * @param includeVoidedLineItems {@code true} to include voided line items, {@code false} to exclude
	 *            them.
	 * @return This BillSearch instance for method chaining.
	 */
	public BillSearch includeVoidedLineItems(boolean includeVoidedLineItems) {
		this.includeVoidedLineItems = includeVoidedLineItems;
		return this;
	}
	
	public Boolean getIncludeVoidedLineItems() {
		return includeVoidedLineItems;
	}
	
	/**
	 * Sets multiple statuses to filter by. When multiple statuses are provided, bills matching any of
	 * the specified statuses will be returned.
	 * 
	 * @param statuses The list of statuses to filter by.
	 * @return This BillSearch instance.
	 */
	public BillSearch setStatuses(List<BillStatus> statuses) {
		this.statuses = statuses;
		return this;
	}
	
	@Override
	public void updateCriteria(Criteria criteria) {
		super.updateCriteria(criteria);
		
		Bill bill = getTemplate();
		if (bill.getCashier() != null) {
			criteria.add(Restrictions.eq("cashier", bill.getCashier()));
		}
		if (bill.getCashPoint() != null) {
			criteria.add(Restrictions.eq("cashPoint", bill.getCashPoint()));
		}
		if (bill.getPatient() != null) {
			criteria.add(Restrictions.eq("patient", bill.getPatient()));
		}
		
		if (patientName != null && !patientName.trim().isEmpty()) {
			List<Patient> matchingPatients = Context.getRegisteredComponent("patientDAO", HibernatePatientDAO.class)
			        .getPatients(patientName, 0, null);
			if (matchingPatients != null && !matchingPatients.isEmpty()) {
				criteria.add(Restrictions.in("patient", matchingPatients));
			} else {
				criteria.add(Restrictions.sqlRestriction("1 = 2"));
			}
		}
		
		if (statuses != null && !statuses.isEmpty()) {
			// Filter by multiple statuses using IN clause
			criteria.add(Restrictions.in("status", statuses));
		} else if (bill.getStatus() != null) {
			criteria.add(Restrictions.eq("status", bill.getStatus()));
		}
		if (fromDate != null) {
			criteria.add(Restrictions.ge("dateCreated", fromDate));
		}
		if (toDate != null) {
			// Set to end of day (23:59:59.999) for inclusive date range
			Calendar cal = Calendar.getInstance();
			cal.setTime(toDate);
			cal.set(Calendar.HOUR_OF_DAY, 23);
			cal.set(Calendar.MINUTE, 59);
			cal.set(Calendar.SECOND, 59);
			cal.set(Calendar.MILLISECOND, 999);
			criteria.add(Restrictions.le("dateCreated", cal.getTime()));
		}
		if (locations != null && !locations.isEmpty()) {
			criteria.createAlias("cashPoint", "cp");
			criteria.add(Restrictions.in("cp.location", locations));
		}
		if (provider != null) {
			DetachedCriteria matchingLineItems = DetachedCriteria.forClass(BillLineItem.class, "li");
			matchingLineItems.createAlias("li.billableService", "bs");
			matchingLineItems.add(Restrictions.eq("bs.provider", provider));
			if (includeVoidedLineItems == null || !includeVoidedLineItems) {
				matchingLineItems.add(Restrictions.eq("li.voided", false));
			}
			matchingLineItems.add(Restrictions.eqProperty("li.bill.id", "this.id"));
			matchingLineItems.setProjection(Projections.id());
			criteria.add(Subqueries.exists(matchingLineItems));
		}
		criteria.addOrder(Order.desc("id"));
	}
	
	public String getPatientName() {
		return patientName;
	}
	
	public void setPatientName(String patientName) {
		this.patientName = patientName;
	}
	
	public Date getFromDate() {
		return fromDate;
	}
	
	public void setFromDate(Date fromDate) {
		this.fromDate = fromDate;
	}
	
	public Date getToDate() {
		return toDate;
	}
	
	public void setToDate(Date toDate) {
		this.toDate = toDate;
	}
	
	public List<Location> getLocations() {
		return locations;
	}
	
	public void setLocations(List<Location> locations) {
		this.locations = locations;
	}
	
	public Provider getProvider() {
		return provider;
	}
	
	public void setProvider(Provider provider) {
		this.provider = provider;
	}
}
