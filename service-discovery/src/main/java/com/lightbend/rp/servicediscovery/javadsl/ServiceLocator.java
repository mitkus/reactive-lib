/*
 * Copyright 2017 Lightbend, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lightbend.rp.servicediscovery.javadsl;

import akka.actor.ActorSystem;
import com.lightbend.rp.servicediscovery.scaladsl.Service;
import com.lightbend.rp.servicediscovery.scaladsl.ServiceLocator$;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ThreadLocalRandom;
import scala.runtime.AbstractFunction1;
import scala.Option;
import scala.collection.JavaConversions;
import scala.collection.immutable.Seq;
import scala.compat.java8.FutureConverters;

public final class ServiceLocator {
    public interface AddressSelection {
        Optional<Service> select(List<Service> addreses);
    }

    public static AddressSelection addressSelectionFirst = addreses ->
            addreses.isEmpty() ?
                    Optional.empty() :
                    Optional.of(addreses.get(0));

    public static AddressSelection addressSelectionRandom = addreses ->
        addreses.isEmpty() ?
            Optional.empty() :
            Optional.of(addreses.get(ThreadLocalRandom.current().nextInt(addreses.size())));

    public static CompletionStage<Optional<Service>> lookup(String name, String endpoint, ActorSystem actorSystem) {
        return lookup(name, endpoint, actorSystem, addressSelectionRandom);
    }

    public static CompletionStage<Optional<Service>> lookup(String name, String endpoint, ActorSystem actorSystem, AddressSelection addressSelection) {
        return FutureConverters.toJava(
                ServiceLocator$
                        .MODULE$
                        .lookupOne(
                                name,
                                endpoint,
                                new AbstractFunction1<Seq<Service>, Option<Service>>() {
                                    @Override
                                    public Option<Service> apply(Seq<Service> addresses) {
                                        Optional<Service> lookup =
                                                addressSelection.select(JavaConversions.seqAsJavaList(addresses));

                                        return Option.apply(lookup.orElse(null));
                                    }
                                },
                                actorSystem)
                        .map(
                                new AbstractFunction1<Option<Service>, Optional<Service>>() {
                                    @Override
                                    public Optional<Service> apply(Option<Service> value) {
                                        return value.isDefined() ? Optional.of(value.get()) : Optional.empty();
                                    }
                                },

                                actorSystem.dispatcher()
                        )
        );

    }
}
