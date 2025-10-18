package com.shopapplication.config;


import com.shopapplication.models.Product;
import com.shopapplication.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class DataSeeder {

    private final ProductRepository productRepository;

    @Bean
    public CommandLineRunner seedProducts() {
        return args -> {
            if (productRepository.count() == 0) {
                Product p1 = Product.builder()
                        .name("Wireless Headphones")
                        .description("Noise-cancelling over-ear headphones with 20 hours battery life.")
                        .price(1200.0)
                        .stock(15)
                        .imageUrl("https://example.com/headphones.jpg")
                        .category("Electronics")
                        .rating(4.5f)
                        .build();

                Product p2 = Product.builder()
                        .name("Smart Watch")
                        .description("Waterproof smartwatch with heart rate monitor and GPS.")
                        .price(950.0)
                        .stock(20)
                        .imageUrl("https://example.com/smartwatch.jpg")
                        .category("Wearables")
                        .rating(4.3f)
                        .build();

                Product p3 = Product.builder()
                        .name("Bluetooth Speaker")
                        .description("Portable Bluetooth speaker with deep bass and 10-hour playtime.")
                        .price(650.0)
                        .stock(30)
                        .imageUrl("https://example.com/speaker.jpg")
                        .category("Audio")
                        .rating(4.6f)
                        .build();

                Product p4 = Product.builder()
                        .name("Laptop Backpack")
                        .description("Water-resistant backpack for 15.6-inch laptops with multiple compartments.")
                        .price(480.0)
                        .stock(40)
                        .imageUrl("https://example.com/backpack.jpg")
                        .category("Accessories")
                        .rating(4.2f)
                        .build();

                Product p5 = Product.builder()
                        .name("Mechanical Keyboard")
                        .description("RGB backlit mechanical keyboard with blue switches.")
                        .price(850.0)
                        .stock(25)
                        .imageUrl("https://example.com/keyboard.jpg")
                        .category("Computer Peripherals")
                        .rating(4.7f)
                        .build();

                productRepository.save(p1);
                productRepository.save(p2);
                productRepository.save(p3);
                productRepository.save(p4);
                productRepository.save(p5);

                System.out.println("✅ Sample products seeded successfully!");
            } else {
                System.out.println("✅ Products already exist — skipping seeding.");
            }
        };
    }
}

