package com.clickmunch.RestaurantService.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import com.clickmunch.RestaurantService.client.AuthClient;
import com.clickmunch.RestaurantService.client.GeoClient;
import com.clickmunch.RestaurantService.client.MenuClient;
import com.clickmunch.RestaurantService.dto.AuthUserResponse;
import com.clickmunch.RestaurantService.dto.CreateRestaurantRequest;
import com.clickmunch.RestaurantService.dto.LocationDto;
import com.clickmunch.RestaurantService.dto.RestaurantCardResponse;
import com.clickmunch.RestaurantService.dto.RestaurantDetailsResponse;
import com.clickmunch.RestaurantService.dto.RestaurantResponse;
import com.clickmunch.RestaurantService.entity.Restaurant;
import com.clickmunch.RestaurantService.entity.RestaurantProfile;
import com.clickmunch.RestaurantService.repository.RestaurantProfileRepository;
import com.clickmunch.RestaurantService.repository.RestaurantRepository;

@Service
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantProfileRepository restaurantProfileRepository;
    private final GeoClient geoClient;
    private final AuthClient authClient;
    private final MenuClient menuClient;

    public RestaurantService(
            RestaurantRepository restaurantRepository,
            RestaurantProfileRepository restaurantProfileRepository,
            GeoClient geoClient,
            AuthClient authClient,
            MenuClient menuClient
    ) {
        this.restaurantRepository = restaurantRepository;
        this.restaurantProfileRepository = restaurantProfileRepository;
        this.geoClient = geoClient;
        this.authClient = authClient;
        this.menuClient = menuClient;
    }

    public RestaurantResponse createRestaurant(CreateRestaurantRequest request) {

        AuthUserResponse restaurantOwner = authClient.getUserDetails(request.ownerId());
        if (restaurantOwner == null) {
            throw new RuntimeException("Owner not found");
        }

        if (!"RESTAURANT_MANAGER".equals(restaurantOwner.role())) {
            throw new HttpClientErrorException(org.springframework.http.HttpStatus.FORBIDDEN, "User is not a restaurant manager");
        }
        Long locationId = geoClient.createLocation(
                request.name(),
                request.latitude(),
                request.longitude()
        );

        var restaurant = new Restaurant();
        restaurant.setOwnerId(request.ownerId());
        restaurant.setName(request.name());
        restaurant.setDescription(request.description());
        restaurant.setPhone(request.phone());
        restaurant.setEmail(request.email());
        restaurant.setImageUrl(request.imageUrl());
        restaurant.setLocationId(locationId);

        var savedRestaurant = restaurantRepository.save(restaurant);

        return new RestaurantResponse(
                savedRestaurant.getId(),
                savedRestaurant.getName(),
                savedRestaurant.getDescription(),
                savedRestaurant.getPhone(),
                savedRestaurant.getEmail(),
                savedRestaurant.getImageUrl(),
                savedRestaurant.getLocationId()
        );
    }

    public RestaurantResponse getRestaurant(Long id) {
        var restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));

        return new RestaurantResponse(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getDescription(),
                restaurant.getPhone(),
                restaurant.getEmail(),
                restaurant.getImageUrl(),
                restaurant.getLocationId()
        );
    }

    public List<RestaurantResponse> listByOwnerId(Long ownerId) {
        List<Restaurant> restaurants = restaurantRepository.findByOwnerId(ownerId);
        return restaurants.stream().map(restaurant -> new RestaurantResponse(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getDescription(),
                restaurant.getPhone(),
                restaurant.getEmail(),
                restaurant.getImageUrl(),
                restaurant.getLocationId()
        )).toList();
    }

    public List<RestaurantResponse> findNearby(Double latitude, Double longitude, Double radiusInKm) {
        List<LocationDto> locationIds = geoClient.findNearbyLocations(latitude, longitude, radiusInKm);
        if (locationIds.isEmpty()) {
            return List.of();
        } else {
            List<Long> locIds = locationIds.stream().map(LocationDto::id).toList();
            List<Restaurant> restaurants = restaurantRepository.findAllByLocationIdIn(locIds);
            return restaurants.stream().map(restaurant -> new RestaurantResponse(
                    restaurant.getId(),
                    restaurant.getName(),
                    restaurant.getDescription(),
                    restaurant.getPhone(),
                    restaurant.getEmail(),
                    restaurant.getImageUrl(),
                    restaurant.getLocationId()
            )).toList();
        }
    }

    public RestaurantDetailsResponse getRestaurantDetails(Long id) {
        var restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));

        var menuCategories = menuClient.getMenuByRestaurant(id);

        return new RestaurantDetailsResponse(
                restaurant.getId(),
                restaurant.getName(),
                geoClient.getAddressById(restaurant.getId()),
                geoClient.getLocationById(restaurant.getId()).latitude(),
                geoClient.getLocationById(restaurant.getId()).longitude(),
                restaurant.getDescription(),
                restaurant.getImageUrl(),
                menuCategories
        );
    }

    public List<RestaurantCardResponse> listRestaurantCards() {
        List<Restaurant> restaurants = restaurantRepository.findAll();
        if (restaurants.isEmpty()) {
            return List.of();
        }

        List<Long> restaurantIds = restaurants.stream().map(Restaurant::getId).toList();
        Map<Long, RestaurantProfile> profilesByRestaurantId = restaurantProfileRepository
                .findByRestaurantIdIn(restaurantIds)
                .stream()
                .collect(Collectors.toMap(RestaurantProfile::getRestaurantId, profile -> profile));

        return restaurants.stream().map(restaurant -> {
            RestaurantProfile profile = profilesByRestaurantId.get(restaurant.getId());

            String category = profile != null && profile.getCategory() != null ? profile.getCategory() : "General";
            String city = profile != null && profile.getCity() != null ? profile.getCity() : "Bogota";
            Double rating = profile != null && profile.getRating() != null ? profile.getRating() : 4.0;
            String deliveryTime = profile != null && profile.getDeliveryTime() != null ? profile.getDeliveryTime() : "30 min";
            String price = profile != null && profile.getAvgPrice() != null ? profile.getAvgPrice() : "$ 0";
            String badge = profile != null ? profile.getBadge() : null;
            Double latitude = profile != null && profile.getLatitude() != null ? profile.getLatitude() : 4.711;
            Double longitude = profile != null && profile.getLongitude() != null ? profile.getLongitude() : -74.0721;

            return new RestaurantCardResponse(
                    restaurant.getId(),
                    restaurant.getName(),
                    restaurant.getImageUrl(),
                    rating,
                    deliveryTime,
                    price,
                    badge,
                    category,
                    city,
                    latitude,
                    longitude
            );
        }).toList();
    }
}

