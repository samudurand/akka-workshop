package com.boldradius.sdf.akka

import akka.actor._

object Receiver {
  def props = Props[Receiver]
}

// Mr Dummy Consumer simply shouts to the log the messages it receives
class Receiver extends Actor with ActorLogging {

  def receive: Receive = {
    case message => log.debug(s"Received the following message: $message")
  }
}
