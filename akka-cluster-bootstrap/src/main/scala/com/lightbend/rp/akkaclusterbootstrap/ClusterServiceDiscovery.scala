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

package com.lightbend.rp.akkaclusterbootstrap

import akka.actor.ActorSystem
import akka.discovery.ServiceDiscovery
import com.lightbend.rp.servicediscovery.scaladsl.ServiceLocator
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class ClusterServiceDiscovery(system: ActorSystem) extends ServiceDiscovery {
  import ServiceDiscovery._
  import system.dispatcher

  def lookup(name: String, resolveTimeout: FiniteDuration): Future[Resolved] = {
    ServiceLocator
      .lookup(name, "akka-mgmt-http")(system)
      .map(services =>
        Resolved(
          name,
          services
            .filter(s => s.uri.getHost != null && s.uri.getPort > 0)
            .map(s => ResolvedTarget(s.uri.getHost, Some(s.uri.getPort)))))
  }
}
