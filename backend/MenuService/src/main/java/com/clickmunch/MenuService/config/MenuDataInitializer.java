package com.clickmunch.MenuService.config;

import com.clickmunch.MenuService.entity.Category;
import com.clickmunch.MenuService.entity.MenuCategory;
import com.clickmunch.MenuService.entity.MenuItem;
import com.clickmunch.MenuService.repository.MenuCategoryRepository;
import com.clickmunch.MenuService.repository.MenuItemRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class MenuDataInitializer implements CommandLineRunner {

    private final MenuCategoryRepository categoryRepo;
    private final MenuItemRepository itemRepo;

    public MenuDataInitializer(MenuCategoryRepository categoryRepo, MenuItemRepository itemRepo) {
        this.categoryRepo = categoryRepo;
        this.itemRepo = itemRepo;
    }

    @Override
    public void run(String... args) {
        if (!categoryRepo.findByRestaurantId(1001L).isEmpty()) return;

        seed(1001L, Category.PLATO, List.of(
            item("Lomo Saltado", "Carne salteada con tomate, cebolla y papas crocantes.", new BigDecimal("29900"),
                "https://images.unsplash.com/photo-1604908176997-431f81918a93?w=900&h=600&fit=crop&auto=format"),
            item("Ceviche Clasico", "Pescado fresco marinado en limon con aji y cilantro.", new BigDecimal("27500"),
                "https://images.unsplash.com/photo-1559737558-2f5a35f4523b?w=900&h=600&fit=crop&auto=format"),
            item("Aji de Gallina", "Pollo deshilachado en salsa cremosa de aji amarillo.", new BigDecimal("25900"),
                "https://images.unsplash.com/photo-1547592180-85f173990554?w=900&h=600&fit=crop&auto=format")
        ));

        seed(1002L, Category.PLATO, List.of(
            item("Arroz Oriental de Pollo", "Arroz salteado al wok con vegetales y salsa de soya.", new BigDecimal("24000"),
                "https://images.unsplash.com/photo-1512058564366-18510be2db19?w=900&h=600&fit=crop&auto=format"),
            item("Pollo Agridulce", "Trozos de pollo crujiente en salsa agridulce.", new BigDecimal("26500"),
                "https://images.unsplash.com/photo-1604908554167-6ed6a6c9f839?w=900&h=600&fit=crop&auto=format"),
            item("Ramen Clasico", "Caldo de cerdo con fideos, huevo y chashu.", new BigDecimal("28000"),
                "https://images.unsplash.com/photo-1569718212165-3a8278d5f624?w=900&h=600&fit=crop&auto=format")
        ));

        seed(1003L, Category.PLATO, List.of(
            item("Burrito Carnal", "Tortilla rellena de carne, frijol, guacamole y queso.", new BigDecimal("22900"),
                "https://images.unsplash.com/photo-1565299585323-38174c4a6c4a?w=900&h=600&fit=crop&auto=format"),
            item("Nachos Supreme", "Nachos con carne, pico de gallo, crema y queso.", new BigDecimal("21500"),
                "https://images.unsplash.com/photo-1601050690597-df0568f70950?w=900&h=600&fit=crop&auto=format"),
            item("Tacos de Barbacoa", "Tres tacos de barbacoa con cebolla y cilantro.", new BigDecimal("19900"),
                "https://images.unsplash.com/photo-1551504734-5ee1c4a1479b?w=900&h=600&fit=crop&auto=format")
        ));

        seed(1004L, Category.PLATO, List.of(
            item("Salmón Nigiri (8 pzas)", "Nigiri de salmon fresco sobre arroz de sushi.", new BigDecimal("32000"),
                "https://images.unsplash.com/photo-1553621042-f6e147245754?w=900&h=600&fit=crop&auto=format"),
            item("Roll California", "Roll de pepino, aguacate y cangrejo con semillas de sesamo.", new BigDecimal("27500"),
                "https://images.unsplash.com/photo-1617196034183-421b4040ed20?w=900&h=600&fit=crop&auto=format"),
            item("Miso Ramen", "Sopa de miso con tofu, algas y fideos.", new BigDecimal("23000"),
                "https://images.unsplash.com/photo-1569718212165-3a8278d5f624?w=900&h=600&fit=crop&auto=format")
        ));

        seed(1005L, Category.PLATO, List.of(
            item("Costillas BBQ", "Costillas de cerdo en salsa BBQ ahumada con papas.", new BigDecimal("42000"),
                "https://images.unsplash.com/photo-1544025162-d76694265947?w=900&h=600&fit=crop&auto=format"),
            item("Picana de Res", "Corte de res a la parrilla con chimichurri y ensalada.", new BigDecimal("48000"),
                "https://images.unsplash.com/photo-1558030006-450675393462?w=900&h=600&fit=crop&auto=format"),
            item("Chorizo Argentino", "Chorizo artesanal a la parrilla con pan y aji.", new BigDecimal("22000"),
                "https://images.unsplash.com/photo-1529193591184-b1d58069ecdd?w=900&h=600&fit=crop&auto=format")
        ));

        seed(1006L, Category.BEBIDA, List.of(
            item("Cappuccino de Especialidad", "Espresso doble con leche vaporizada y arte latte.", new BigDecimal("12000"),
                "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=900&h=600&fit=crop&auto=format"),
            item("Tostada Francesa con Frutas", "Pan brioche tostado con frutas frescas y miel.", new BigDecimal("18500"),
                "https://images.unsplash.com/photo-1484723091739-30a097e8f929?w=900&h=600&fit=crop&auto=format"),
            item("Cold Brew", "Cafe de extraccion lenta servido frio con hielo.", new BigDecimal("10000"),
                "https://images.unsplash.com/photo-1461023058943-07fcbe16d735?w=900&h=600&fit=crop&auto=format")
        ));

        seed(1007L, Category.POSTRE, List.of(
            item("Torta de Chocolate", "Bizcocho humedo de chocolate con ganache y fresas.", new BigDecimal("18000"),
                "https://images.unsplash.com/photo-1578985545062-69928b1d9587?w=900&h=600&fit=crop&auto=format"),
            item("Cheesecake de Maracuya", "Base de galleta con crema de maracuya y coulis.", new BigDecimal("16500"),
                "https://images.unsplash.com/photo-1533134242443-d4fd215305ad?w=900&h=600&fit=crop&auto=format"),
            item("Macarons Surtidos (6 pzas)", "Macarons franceses en sabores variados.", new BigDecimal("22000"),
                "https://images.unsplash.com/photo-1558326567-98ae2405596b?w=900&h=600&fit=crop&auto=format")
        ));

        seed(1008L, Category.PLATO, List.of(
            item("Smash Burger Doble", "Doble carne aplastada, queso americano, pepinillos y salsa especial.", new BigDecimal("28000"),
                "https://images.unsplash.com/photo-1550547660-d9450f859349?w=900&h=600&fit=crop&auto=format"),
            item("Crispy Chicken Burger", "Filete de pollo crocante con col morada y mayo de chipotle.", new BigDecimal("25000"),
                "https://images.unsplash.com/photo-1606755962773-d324e0a13086?w=900&h=600&fit=crop&auto=format"),
            item("Papas Gajo con Dip", "Papas en gajo al horno con dip de queso cheddar.", new BigDecimal("12000"),
                "https://images.unsplash.com/photo-1573080496219-bb080dd4f877?w=900&h=600&fit=crop&auto=format")
        ));

        seed(1009L, Category.PLATO, List.of(
            item("Spaghetti Carbonara", "Pasta con panceta, huevo, parmesano y pimienta negra.", new BigDecimal("29000"),
                "https://images.unsplash.com/photo-1621996346565-e3dbc353d2e5?w=900&h=600&fit=crop&auto=format"),
            item("Lasagna Bolognesa", "Capas de pasta con ragu de carne y bechamel gratinada.", new BigDecimal("32000"),
                "https://images.unsplash.com/photo-1574894709920-11b28e7367e3?w=900&h=600&fit=crop&auto=format"),
            item("Risotto de Funghi", "Arroz arborio cremoso con hongos porcini y parmesano.", new BigDecimal("31000"),
                "https://images.unsplash.com/photo-1476124369491-e7addf5db371?w=900&h=600&fit=crop&auto=format")
        ));

        seed(1010L, Category.PLATO, List.of(
            item("Pollo Asado Entero", "Pollo a la brasa con papas amarillas y aji verde.", new BigDecimal("38000"),
                "https://images.unsplash.com/photo-1600891964599-f61ba0e24092?w=900&h=600&fit=crop&auto=format"),
            item("Medio Pollo con Ensalada", "Media presa de pollo asado con ensalada fresca.", new BigDecimal("22000"),
                "https://images.unsplash.com/photo-1598514983318-2f64f8f4796c?w=900&h=600&fit=crop&auto=format"),
            item("Alitas Picantes (10 pzas)", "Alitas de pollo marinadas en salsa picante con ranch.", new BigDecimal("24000"),
                "https://images.unsplash.com/photo-1567620832903-9fc6debc209f?w=900&h=600&fit=crop&auto=format")
        ));
    }

    private void seed(Long restaurantId, Category category, List<MenuItem> items) {
        MenuCategory cat = new MenuCategory();
        cat.setRestaurantId(restaurantId);
        cat.setCategory(category);
        MenuCategory saved = categoryRepo.save(cat);
        items.forEach(i -> {
            i.setCategoryId(saved.getId());
            itemRepo.save(i);
        });
    }

    private MenuItem item(String name, String description, BigDecimal price, String imageUrl) {
        MenuItem m = new MenuItem();
        m.setName(name);
        m.setDescription(description);
        m.setPrice(price);
        m.setImageUrl(imageUrl);
        return m;
    }
}
