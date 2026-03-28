package ru.blps.lab_1.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ru.blps.lab_1.entity.AppUser;
import ru.blps.lab_1.entity.Courier;
import ru.blps.lab_1.entity.Restaurant;
import ru.blps.lab_1.repository.AppUserRepository;
import ru.blps.lab_1.repository.CourierRepository;
import ru.blps.lab_1.repository.RestaurantRepository;

@Component
@ConditionalOnProperty(name = "app.seed-data", havingValue = "true", matchIfMissing = true)
public class ReferenceDataLoader implements ApplicationRunner {

    private final AppUserRepository appUserRepository;
    private final CourierRepository courierRepository;
    private final RestaurantRepository restaurantRepository;

    public ReferenceDataLoader(
        AppUserRepository appUserRepository,
        CourierRepository courierRepository,
        RestaurantRepository restaurantRepository
    ) {
        this.appUserRepository = appUserRepository;
        this.courierRepository = courierRepository;
        this.restaurantRepository = restaurantRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (int i = 1; i <= 5; i++) {
            String login = "client" + i;
            if (!appUserRepository.existsByUsername(login)) {
                appUserRepository.save(new AppUser(login, "Клиент " + i, "7999111111" + i, true));
            }
        }
        for (int i = 1; i <= 5; i++) {
            String login = "courier" + i;
            if (!courierRepository.existsByLogin(login)) {
                courierRepository.save(new Courier("Курьер " + i, "7999222222" + i, "Москва", login));
            }
        }
        for (int i = 1; i <= 5; i++) {
            String login = "restaurant" + i;
            if (!restaurantRepository.existsByLogin(login)) {
                restaurantRepository.save(new Restaurant("Ресторан " + i, "Москва", "ул. Примерная, " + i, login));
            }
        }
    }
}
