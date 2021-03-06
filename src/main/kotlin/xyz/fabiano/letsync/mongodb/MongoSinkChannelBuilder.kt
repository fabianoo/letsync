package xyz.fabiano.letsync.mongodb

import com.mongodb.ConnectionString
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.coroutine.CoroutineDatabase
import xyz.fabiano.letsync.api.LetSyncSink
import xyz.fabiano.letsync.dsl.LetSyncSinkBuilder
import xyz.fabiano.letsync.dsl.LetSyncBuilder
import xyz.fabiano.letsync.dsl.SyncDsl

@SyncDsl
class MongoSinkChannelBuilder<C : Any> {
    private var host = { "localhost" }
    private var user = { "admin" }
    private var databaseName = { "test" }
    private var password : (() -> String)? = null

    private var connectionString : ConnectionString? = null

    private var database : (() -> CoroutineDatabase)? = null

    fun mongoUrl(block: () -> String) {
        connectionString = ConnectionString(block.invoke())
    }

    fun host(block : () -> String) {
        host = block
    }

    fun databaseName(block : () -> String) {
        databaseName = block
    }

    fun database(block : () -> CoroutineDatabase) {
        database = block
    }

    fun user(block : () -> String) {
        user = block
    }

    fun password(block : () -> String) {
        password = block
    }

    fun replicaSet(block : () -> String) {
        // TODO
        throw NotImplementedError()
    }

    fun database(): CoroutineDatabase {
        return database?.invoke() ?: client().database()
    }


    private fun client() : MongoClient {
        val url = connectionString ?: buildConnectionString()
        return MongoClient(url, databaseName.invoke(), user.invoke(), password?.invoke())
    }

    fun buildWithCollection(collection: CoroutineCollection<C>) : LetSyncSink<C> {
        return MongoCollectionSinkChannel(collection)
    }

    inline fun <reified T : Any> build() : LetSyncSink<T> {
        val coll= database().getCollection<T>()
        return MongoCollectionSinkChannel(coll)
    }

    private fun buildConnectionString() : ConnectionString {
        return ConnectionString("mongodb://${host.invoke()}")
    }
}

inline fun <reified T : Any> LetSyncSinkBuilder<T>.mongo(block: MongoSinkChannelBuilder<T>.() -> Unit) {
    val channel = MongoSinkChannelBuilder<T>().apply(block).build<T>()
    this.with { channel }
}

inline fun <R, reified S : Any> LetSyncBuilder<R, S>.sinkOnMongo(block: MongoSinkChannelBuilder<S>.() -> Unit) {
    this.sink(MongoSinkChannelBuilder<S>().apply(block).build())
}

inline fun <reified T : Any> MongoSinkChannelBuilder<T>.collection(collection : () -> CoroutineCollection<T>) {
    val coll = collection.invoke()
    LetSyncSinkBuilder<T>().with { this.buildWithCollection(coll) }
}