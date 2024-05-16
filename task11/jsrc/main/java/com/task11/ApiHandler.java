package com.task11;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.RetentionSetting;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminSetUserPasswordRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InvalidParameterException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUserPoolClientsRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUserPoolsRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.task11.Constants.CLIENT_NAME;
import static com.task11.Constants.HH_MM;
import static com.task11.Constants.REQUEST_PARSING_ERROR;
import static com.task11.Constants.RESERVATIONS_TABLE_NAME;
import static com.task11.Constants.RESERVATIONS_URI;
import static com.task11.Constants.SIGN_IN_URI;
import static com.task11.Constants.SIGN_UP_URI;
import static com.task11.Constants.TABLES_TABLE_ID_URI;
import static com.task11.Constants.TABLES_TABLE_NAME;
import static com.task11.Constants.TABLES_URI;
import static com.task11.Constants.TABLE_ID;
import static com.task11.Constants.USER_POOL_NAME;

@LambdaHandler(lambdaName = "api_handler",
	roleName = "api_handler-role",
	logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
public class ApiHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

	private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(HH_MM);
	private final AmazonDynamoDB dynamoDB = AmazonDynamoDBClientBuilder.standard().withRegion(Regions.EU_CENTRAL_1).build();
	private final CognitoIdentityProviderClient cognitoClient = CognitoIdentityProviderClient.builder().region(Region.EU_CENTRAL_1).build();
	private final ObjectMapper mapper = new ObjectMapper();
	private String userPoolId;
	private String clientId;
	private final Map<String, String> defaultHeaders = Map.of(
		"Access-Control-Allow-Headers", "Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token",
		"Access-Control-Allow-Origin", "*",
		"Access-Control-Allow-Methods", "*",
		"Accept-Version", "*"
	);

	public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent event, final Context context) {
		final var resource = event.getResource();
		final var httpMethod = event.getHttpMethod();
		final var pathParameters = event.getPathParameters();

		System.out.println("Event: " + event);

		if (resource.equals(SIGN_UP_URI) && httpMethod.equals("POST")) {
			return handleSignUpRequest(event);
		} else if (resource.equals(SIGN_IN_URI) && httpMethod.equals("POST")) {
			return handleSignInRequest(event);
		} else if (resource.equals(TABLES_URI) && httpMethod.equals("POST")) {
			return handleTablesPostRequest(event);
		} else if (resource.equals(TABLES_URI) && httpMethod.equals("GET")) {
			return handleTablesGetAllRequest();
		} else if (resource.equals(TABLES_TABLE_ID_URI) && httpMethod.equals("GET") && pathParameters.get(TABLE_ID) != null) {
			return handleTablesGetByIdRequest(pathParameters);
		} else if (resource.equals(RESERVATIONS_URI) && httpMethod.equals("POST")) {
			return handleReservationsPostRequest(event);
		} else if (resource.equals(RESERVATIONS_URI) && httpMethod.equals("GET")) {
			return handleReservationsGetAllRequest();
		} else {
			return new APIGatewayProxyResponseEvent()
				.withStatusCode(400)
				.withHeaders(defaultHeaders)
				.withBody("Invalid resource or http method");
		}
	}

	private void setUserPoolId() {
		if (userPoolId == null) {
			var userPools = cognitoClient.listUserPools(
				ListUserPoolsRequest.builder().maxResults(50).build()).userPools();
			for (var userPool : userPools) {
				if (userPool.name().equals(USER_POOL_NAME)) {
					userPoolId = userPool.id();
					break;
				}
			}
		}
	}

	private void setClientId() {
		if (clientId == null && userPoolId != null) {
			var clientsResponse = cognitoClient.listUserPoolClients(
				ListUserPoolClientsRequest.builder().userPoolId(userPoolId).maxResults(50).build());
			for (var client : clientsResponse.userPoolClients()) {
				if (client.clientName().equals(CLIENT_NAME)) {
					clientId = client.clientId();
					break;
				}
			}
		}
	}

	private boolean isValidPassword(final String password) {
		final String regex = "[a-zA-Z0-9?=$%^*].{11,}";
		final Pattern pattern = Pattern.compile(regex);
		final Matcher matcher = pattern.matcher(password);
		final boolean result = matcher.matches();
		System.out.println("Is valid password: " + result);
		return result;
	}

	private APIGatewayProxyResponseEvent handleSignUpRequest(final APIGatewayProxyRequestEvent event) {
		try {
			final SignUpRequest request = mapper.readValue(event.getBody(), SignUpRequest.class);
			final String password = request.getPassword();
			if (!isValidPassword(password)) {
				return new APIGatewayProxyResponseEvent()
					.withStatusCode(400)
					.withHeaders(defaultHeaders)
					.withBody("Invalid password");
			}

			setUserPoolId();
			final AdminCreateUserRequest adminCreateUserRequest = AdminCreateUserRequest.builder()
				.userPoolId(userPoolId)
				.username(request.getEmail())
				.userAttributes(
					AttributeType.builder().name("email").value(request.getEmail()).build(),
					AttributeType.builder().name("given_name").value(request.getFirstName()).build(),
					AttributeType.builder().name("family_name").value(request.getLastName()).build()
				)
				.messageAction("SUPPRESS")
				.build();
			final var userName = cognitoClient.adminCreateUser(adminCreateUserRequest).user().username();

			final AdminSetUserPasswordRequest passwordRequest = AdminSetUserPasswordRequest.builder()
				.userPoolId(userPoolId)
				.username(userName)
				.password(password)
				.permanent(true)
				.build();
			cognitoClient.adminSetUserPassword(passwordRequest);

			return new APIGatewayProxyResponseEvent()
				.withStatusCode(200)
				.withHeaders(defaultHeaders)
				.withBody("SignUp success");
		} catch (final JsonProcessingException e) {
			System.out.println("Returning 400 due to JsonProcessingException: " + e.getMessage());
			return new APIGatewayProxyResponseEvent()
				.withStatusCode(400)
				.withHeaders(defaultHeaders)
				.withBody(REQUEST_PARSING_ERROR + e.getMessage());
		} catch (final InvalidParameterException e) {
			System.out.println("Returning 400 due to InvalidParameterException: " + e.getMessage());
			return new APIGatewayProxyResponseEvent()
				.withStatusCode(400)
				.withHeaders(defaultHeaders)
				.withBody("Wrong request params:" + e.getMessage());
		}
	}

	private APIGatewayProxyResponseEvent handleSignInRequest(final APIGatewayProxyRequestEvent event) {
		try {
			setUserPoolId();
			setClientId();
			final SignInRequest request = mapper.readValue(event.getBody(), SignInRequest.class);
			final AdminInitiateAuthRequest adminInitiateAuthRequest = AdminInitiateAuthRequest.builder()
				.userPoolId(userPoolId)
				.clientId(clientId)
				.authFlow(AuthFlowType.ADMIN_USER_PASSWORD_AUTH)
				.authParameters(Map.of("USERNAME", request.getEmail(), "PASSWORD", request.getPassword()))
				.build();
			final AdminInitiateAuthResponse response = cognitoClient.adminInitiateAuth(adminInitiateAuthRequest);
			return new APIGatewayProxyResponseEvent()
				.withStatusCode(200)
				.withHeaders(defaultHeaders)
				.withBody(mapper.writeValueAsString(Map.of("accessToken", response.authenticationResult().idToken())));
		} catch (final JsonProcessingException e) {
			return new APIGatewayProxyResponseEvent()
				.withStatusCode(400)
				.withHeaders(defaultHeaders)
				.withBody(REQUEST_PARSING_ERROR + e.getMessage());
		} catch (final UserNotFoundException e) {
			return new APIGatewayProxyResponseEvent()
				.withStatusCode(400)
				.withHeaders(defaultHeaders)
				.withBody("User not found: " + e.getMessage());
		}
	}

	private Table dynamoDBItemToTable(final Map<String, AttributeValue> item) {
		return new Table(
			Integer.parseInt(item.get("id").getS()),
			Integer.parseInt(item.get("number").getN()),
			Integer.parseInt(item.get("places").getN()),
			item.get("isVip").getBOOL(),
			Integer.parseInt(item.get("minOrder").getN())
		);
	}

	private List<Table> getAllTables() {
		final ScanResult scanResult = dynamoDB.scan(new ScanRequest().withTableName(TABLES_TABLE_NAME));
		final List<Table> tables = new ArrayList<>();
		for (var item : scanResult.getItems()) {
			tables.add(dynamoDBItemToTable(item));
		}
		return tables;
	}

	private APIGatewayProxyResponseEvent handleTablesGetAllRequest() {
		try {
			final var tables = getAllTables();
			final Map<String, List<Table>> responseMap = new HashMap<>();
			responseMap.put("tables", tables);
			return new APIGatewayProxyResponseEvent()
				.withStatusCode(200)
				.withHeaders(defaultHeaders)
				.withBody(mapper.writeValueAsString(responseMap));
		} catch (final Exception e) {
			return new APIGatewayProxyResponseEvent()
				.withStatusCode(500)
				.withHeaders(defaultHeaders)
				.withBody("Error fetching tables: " + e.getMessage());
		}
	}

	private APIGatewayProxyResponseEvent handleTablesGetByIdRequest(final Map<String, String> pathParameters) {
		try {
			final GetItemRequest getItemRequest = new GetItemRequest()
				.withTableName(TABLES_TABLE_NAME)
				.addKeyEntry("id", new AttributeValue().withS(pathParameters.get(TABLE_ID)));
			final GetItemResult result = dynamoDB.getItem(getItemRequest);
			final var table = dynamoDBItemToTable(result.getItem());
			return new APIGatewayProxyResponseEvent()
				.withStatusCode(200)
				.withHeaders(defaultHeaders)
				.withBody(mapper.writeValueAsString(table));
		} catch (final JsonProcessingException e) {
			return new APIGatewayProxyResponseEvent()
				.withStatusCode(500)
				.withHeaders(defaultHeaders)
				.withBody("Response parsing error" + e.getMessage());
		}
	}

	private APIGatewayProxyResponseEvent handleTablesPostRequest(final APIGatewayProxyRequestEvent event) {
		try {
			final Table table = mapper.readValue(event.getBody(), Table.class);
			final Map<String, AttributeValue> item = new HashMap<>();
			item.put("id", new AttributeValue().withS(String.valueOf(table.getId())));
			item.put("number", new AttributeValue().withN(String.valueOf(table.getNumber())));
			item.put("places", new AttributeValue().withN(String.valueOf(table.getPlaces())));
			item.put("isVip", new AttributeValue().withBOOL(table.getIsVip()));
			item.put("minOrder", new AttributeValue().withN(String.valueOf(table.getMinOrder())));
			dynamoDB.putItem(new PutItemRequest().withTableName(TABLES_TABLE_NAME).withItem(item));
			return new APIGatewayProxyResponseEvent()
				.withStatusCode(200)
				.withHeaders(defaultHeaders)
				.withBody(mapper.writeValueAsString(Map.of("id", table.getId())));
		} catch (final JsonProcessingException e) {
			return new APIGatewayProxyResponseEvent()
				.withStatusCode(500)
				.withHeaders(defaultHeaders)
				.withBody(REQUEST_PARSING_ERROR + e.getMessage());
		} catch (final Exception e) {
			return new APIGatewayProxyResponseEvent()
				.withStatusCode(500)
				.withHeaders(defaultHeaders)
				.withBody("Error creating table: " + e.getMessage());
		}
	}

	private List<Reservation> getAllReservations() {
		final ScanResult scanResult = dynamoDB.scan(new ScanRequest().withTableName(RESERVATIONS_TABLE_NAME));
		final List<Reservation> reservations = new ArrayList<>();
		for (var item : scanResult.getItems()) {
			reservations.add(dynamoDBItemToReservation(item));
		}
		return reservations;
	}

	private LocalTime parseTime(final String str) {
		return LocalTime.parse(str, timeFormatter);
	}

	private boolean isValidReservation(final Reservation reservation) {
		final var tables = getAllTables();
		if (tables.stream().filter(table -> table.getNumber() == reservation.getTableNumber()).findAny().isEmpty()) {
			return false;
		}

		final var allReservations = getAllReservations();
		final var sameDateSameTableReservations = allReservations.stream().filter(
			r -> r.getTableNumber() == reservation.getTableNumber() && r.getDate().equals(reservation.getDate())
		).collect(Collectors.toList());
		if (!sameDateSameTableReservations.isEmpty()) {
			final var reservationStart = parseTime(reservation.getSlotTimeStart());
			final var reservationEnd = parseTime(reservation.getSlotTimeEnd());
			final var intersections = sameDateSameTableReservations.stream().filter(
					r -> parseTime(r.getSlotTimeStart()).isAfter(reservationStart)
						&& parseTime(r.getSlotTimeStart()).isBefore(reservationEnd)
					|| parseTime(r.getSlotTimeEnd()).isAfter(reservationStart)
						&& parseTime(r.getSlotTimeEnd()).isBefore(reservationEnd)
					|| parseTime(r.getSlotTimeStart()).equals(reservationStart)
						|| parseTime(r.getSlotTimeEnd()).equals(reservationEnd)
				).collect(Collectors.toList());
			return intersections.isEmpty();
		}
		return true;
	}

	private APIGatewayProxyResponseEvent handleReservationsPostRequest(final APIGatewayProxyRequestEvent event) {
		try {
			final Reservation reservation = mapper.readValue(event.getBody(), Reservation.class);
			if (!isValidReservation(reservation)) {
				return new APIGatewayProxyResponseEvent()
					.withStatusCode(400)
					.withHeaders(defaultHeaders)
					.withBody("Wrong reservation");
			}

			final Map<String, AttributeValue> item = new HashMap<>();
			final String id = UUID.randomUUID().toString();
			item.put("id", new AttributeValue().withS(id));
			item.put("tableNumber",  new AttributeValue().withN(String.valueOf(reservation.getTableNumber())));
			item.put("clientName", new AttributeValue().withS(reservation.getClientName()));
			item.put("phoneNumber", new AttributeValue().withS(reservation.getPhoneNumber()));
			item.put("date", new AttributeValue().withS(reservation.getDate()));
			item.put("slotTimeStart", new AttributeValue().withS(reservation.getSlotTimeStart()));
			item.put("slotTimeEnd", new AttributeValue().withS(reservation.getSlotTimeEnd()));
			dynamoDB.putItem(new PutItemRequest().withTableName(RESERVATIONS_TABLE_NAME).withItem(item));
			return new APIGatewayProxyResponseEvent()
				.withStatusCode(200)
				.withHeaders(defaultHeaders)
				.withBody(mapper.writeValueAsString(Map.of("reservationId", id)));
		} catch (final JsonProcessingException e) {
			return new APIGatewayProxyResponseEvent()
				.withStatusCode(400)
				.withHeaders(defaultHeaders)
				.withBody(REQUEST_PARSING_ERROR + e.getMessage());
		} catch (final Exception e) {
			return new APIGatewayProxyResponseEvent()
				.withStatusCode(500)
				.withHeaders(defaultHeaders)
				.withBody("Error creating reservation: " + e.getMessage());
		}
	}

	private Reservation dynamoDBItemToReservation(final Map<String, AttributeValue> item) {
		return new Reservation(
			Integer.parseInt(item.get("tableNumber").getN()),
			item.get("clientName").getS(),
			item.get("phoneNumber").getS(),
			item.get("date").getS(),
			item.get("slotTimeStart").getS(),
			item.get("slotTimeEnd").getS()
		);
	}

	private APIGatewayProxyResponseEvent handleReservationsGetAllRequest() {
		try {
			final var reservations = getAllReservations();
			final Map<String, List<Reservation>> responseMap = new HashMap<>();
			responseMap.put("reservations", reservations);
			return new APIGatewayProxyResponseEvent()
				.withStatusCode(200)
				.withHeaders(defaultHeaders)
				.withBody(mapper.writeValueAsString(responseMap));
		} catch (final Exception e) {
			return new APIGatewayProxyResponseEvent()
				.withStatusCode(500)
				.withHeaders(defaultHeaders)
				.withBody("Error fetching reservations: " + e.getMessage());
		}
	}
}
