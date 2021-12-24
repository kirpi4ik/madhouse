import eu.devexpert.madhouse.async.mqtt.handlers.MQTTMessageHandler
import eu.devexpert.madhouse.auth.ClaimProvider
import eu.devexpert.madhouse.graphql.GQLSchemaFactory
import eu.devexpert.madhouse.graphql.GraphQLGenerator
import eu.devexpert.madhouse.graphql.fetchers.Mutation

import eu.devexpert.madhouse.async.socket.WebSocketConfig
import eu.devexpert.madhouse.listener.domain.UserPasswordEncoderListener
import eu.devexpert.madhouse.telegram.TelegramBotHandler
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

beans = {
    passwordEncoder(BCryptPasswordEncoder)
    userPasswordEncoderListener(UserPasswordEncoderListener)
    graphQLSchemaGenerator(GQLSchemaFactory) {
        deleteResponseHandler = ref("graphQLDeleteResponseHandler")
        namingConvention = ref("graphQLEntityNamingConvention")
        typeManager = ref("graphQLTypeManager")
        dataBinderManager = ref("graphQLDataBinderManager")
        dataFetcherManager = ref("graphQLDataFetcherManager")
        interceptorManager = ref("graphQLInterceptorManager")
        paginationResponseHandler = ref("graphQLPaginationResponseHandler")
        serviceManager = ref("graphQLServiceManager")

        dateFormats = '#{grailsGraphQLConfiguration.getDateFormats()}'
        dateFormatLenient = '#{grailsGraphQLConfiguration.getDateFormatLenient()}'
        listArguments = '#{grailsGraphQLConfiguration.getListArguments()}'
        gqlSchema = "classpath:schema.graphqls"
    }
    mutation(Mutation)
    graphQLGenrator(GraphQLGenerator) {
        schema = ref("graphQLSchema")
    }
    graphQL(graphQLGenrator: "generate")
    customClaimProvider(ClaimProvider)

    threadMetrics(JvmThreadMetrics)
//
    webSocketConfig(WebSocketConfig)
    mQTTMessageHandler(MQTTMessageHandler)
    mQTTMessageHandler(MQTTMessageHandler)
    telegramBotHandler(TelegramBotHandler)
}