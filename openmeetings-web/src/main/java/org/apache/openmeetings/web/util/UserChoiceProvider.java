/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License") +  you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.openmeetings.web.util;

import static org.apache.openmeetings.web.app.Application.getBean;
import static org.apache.openmeetings.web.app.WebSession.getUserId;

import java.util.HashMap;
import java.util.Map;

import org.apache.openmeetings.db.dao.user.UserDao;
import org.apache.openmeetings.db.entity.user.User;
import org.apache.openmeetings.db.entity.user.User.Type;
import org.apache.openmeetings.db.util.FormatHelper;
import org.apache.wicket.extensions.validation.validator.RfcCompliantEmailAddressValidator;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.validation.Validatable;
import org.wicketstuff.select2.Response;

import com.github.openjson.JSONException;
import com.github.openjson.JSONStringer;

public class UserChoiceProvider extends RestrictiveChoiceProvider<User> {
	private static final long serialVersionUID = 1L;
	private final static int PAGE_SIZE = 10;
	private final Map<String, User> newContacts = new HashMap<>();

	public static User getUser(String value) {
		User u = null;
		if (!Strings.isEmpty(value)) {
			//FIXME refactor this
			String email = null;
			String fName = null;
			String lName = null;
			int idx = value.indexOf('<');
			if (idx > -1) {
				int idx1 = value.indexOf('>', idx);
				if (idx1 > -1) {
					email = value.substring(idx + 1, idx1);

					String name = value.substring(0, idx).replace("\"", "");
					int idx2 = name.indexOf(' ');
					if (idx2 > -1) {
						fName = name.substring(0, idx2);
						lName = name.substring(idx2 + 1);
					} else {
						fName = "";
						lName = name;
					}

				}
			} else {
				email = value;
			}
			if (!Strings.isEmpty(email)) {
				Validatable<String> valEmail = new Validatable<>(email);
				RfcCompliantEmailAddressValidator.getInstance().validate(valEmail);
				if (valEmail.isValid()) {
					u = getBean(UserDao.class).getContact(email, fName, lName, getUserId());
				}
			}
		}
		return u;
	}

	@Override
	public String toId(User u) {
		String id = "" + u.getId();
		if (u.getId() == null) {
			newContacts.put(u.getLogin(), u);
			id = u.getLogin();
		}
		return id;
	}

	@Override
	public String getDisplayValue(User object) {
		return FormatHelper.formatUser(object, true);
	}

	@Override
	public void query(String term, int page, Response<User> response) {
		User c = getUser(term);
		if (c != null) {
			response.add(c);
		}
		UserDao dao = getBean(UserDao.class);
		response.addAll(dao.get(term, page * PAGE_SIZE, PAGE_SIZE, null, true, getUserId()));

		response.setHasMore(page < dao.countUsers(term, getUserId()) / PAGE_SIZE);
	}

	@Override
	public User fromId(String id) {
		User u = null;
		if (newContacts.containsKey(id)) {
			u = newContacts.get(id);
		} else {
			u = getBean(UserDao.class).get(Long.valueOf(id));
		}
		return u;
	}

	@Override
	public void toJson(User choice, JSONStringer stringer) throws JSONException {
		super.toJson(choice, stringer);
		stringer.key("contact").value(choice.getType() == Type.contact);
	}

	@Override
	public void detach() {
	}
}
