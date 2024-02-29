package com.example.helloworld.aggregate

import akka.actor.typed.Behavior
import akka.cluster.sharding.typed.scaladsl.{EntityContext, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.EventSourcedBehavior

object WalletBehavior {

  val EntityKey = EntityTypeKey[Command]("Wallets")

  /**
   * Given a sharding [[EntityContext]] this function produces an Akka [[Behavior]] for the aggregate.
   */
  def create(entityContext: EntityContext[AchTransactionCommand]): Behavior[AchTransactionCommand] = {
    val persistenceId: PersistenceId = PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId)

    create(persistenceId)
      .withTagger(
        // Using Akka Persistence Typed in Lagom requires tagging your events
        // in Lagom-compatible way so Lagom ReadSideProcessors and TopicProducers
        // can locate and follow the event streams.
        AkkaTaggerAdapter.fromLagom(entityContext, AchTransactionPersistentEvent.Tag)
      )

  }

  /*
   * This method is extracted to write unit tests that are completely independendant to Akka Cluster.
   */
  private[infrastructure] def create(persistenceId: PersistenceId) = EventSourcedBehavior
    .withEnforcedReplies[AchTransactionCommand, AchTransactionPersistentEvent, AchTransactionState](
      persistenceId = persistenceId,
      emptyState = AchTransactionState.initial,
      commandHandler = (achTransaction, cmd) => achTransaction.applyCommand(cmd),
      eventHandler = (achTransaction, evt) => achTransaction.applyEvent(achTransaction, evt)
    )
}

sealed trait AchTransactionState {
  def applyCommand(cmd: AchTransactionCommand): ReplyEffect[AchTransactionPersistentEvent, AchTransactionState]

  def applyEvent(achTransactionState: AchTransactionState,
                 evt: AchTransactionPersistentEvent): AchTransactionState
}
