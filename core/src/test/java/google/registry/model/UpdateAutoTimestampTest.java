// Copyright 2017 The Nomulus Authors. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package google.registry.model;

import static com.google.common.truth.Truth.assertThat;
import static google.registry.model.ofy.ObjectifyService.ofy;
import static google.registry.persistence.transaction.TransactionManagerFactory.tm;
import static org.joda.time.DateTimeZone.UTC;

import com.googlecode.objectify.annotation.Entity;
import google.registry.model.common.CrossTldSingleton;
import google.registry.testing.AppEngineRule;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/** Unit tests for {@link UpdateAutoTimestamp}. */
public class UpdateAutoTimestampTest {

  @RegisterExtension
  public final AppEngineRule appEngine =
      AppEngineRule.builder()
          .withDatastoreAndCloudSql()
          .withOfyTestEntities(UpdateAutoTimestampTestObject.class)
          .build();

  /** Timestamped class. */
  @Entity(name = "UatTestEntity")
  public static class UpdateAutoTimestampTestObject extends CrossTldSingleton {
    UpdateAutoTimestamp updateTime = UpdateAutoTimestamp.create(null);
  }

  private UpdateAutoTimestampTestObject reload() {
    return ofy().load().entity(new UpdateAutoTimestampTestObject()).now();
  }

  @Test
  void testSaveSetsTime() {
    DateTime transactionTime =
        tm().transact(
                () -> {
                  UpdateAutoTimestampTestObject object = new UpdateAutoTimestampTestObject();
                  assertThat(object.updateTime.timestamp).isNull();
                  ofy().save().entity(object);
                  return tm().getTransactionTime();
                });
    ofy().clearSessionCache();
    assertThat(reload().updateTime.timestamp).isEqualTo(transactionTime);
  }

  @Test
  void testResavingOverwritesOriginalTime() {
    DateTime transactionTime =
        tm().transact(
                () -> {
                  UpdateAutoTimestampTestObject object = new UpdateAutoTimestampTestObject();
                  object.updateTime = UpdateAutoTimestamp.create(DateTime.now(UTC).minusDays(1));
                  ofy().save().entity(object);
                  return tm().getTransactionTime();
                });
    ofy().clearSessionCache();
    assertThat(reload().updateTime.timestamp).isEqualTo(transactionTime);
  }
}
