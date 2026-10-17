## API päätepisteet

API palvelee verkkokaupan asiakasta. Kaikki pyynnöt ja vastaukset ovat JSON muodossa. Sivutetut listat palautetaan muodossa:

```json
{ "content": [ ... ], "page": 0, "size": 20, "totalElements": 115, "totalPages": 6 }
```

Virheissä palautetaan tilakoodi ja viesti, esim. 400 puuttuva tieto, 404 ei löydy, 409 varasto ei riitä tai tilaus väärässä tilassa:

```json
{ "status": 409, "error": "Conflict", "message": "Not enough stock for product 3", "path": "/api/orders" }
```


### Tuotteet

**GET /api/products**
Tuotelistaus. Vapaaehtoiset suodattimet `categoryId`, `minPrice`, `maxPrice`, `inStock`, `name`, järjestys `sort` (id, price, price_desc, name) ja sivutus `page`, `size`. Palauttaa sivutetun listan tuotteita. Hinta on tämän päivän hinta, kampanjahinta jos sellainen on voimassa.

```
GET /api/products?categoryId=3&maxPrice=100&inStock=true&sort=price
```
```json
{
  "content": [
    {
      "id": 940, "name": "Teltta", "description": "Kahden hengen teltta",
      "price": 98.92, "listPrice": 98.92, "onCampaign": false, "inStock": true,
      "categoryId": 3, "categoryName": "Retkeily", "supplierName": "Toimittaja 12 Oy"
    }
  ],
  "page": 0, "size": 20, "totalElements": 18, "totalPages": 1
}
```

**GET /api/products/search?q=teksti**
Tekstihaku nimestä ja kuvauksesta, osuvimmat ensin. Palauttaa listan tuotteita (max 20), sama tuotemuoto kuin yllä.

**GET /api/products/{id}**
Yhden tuotteen tiedot ja voimassa olevat tai tulevat kampanjahinnat.

```json
{
  "product": { "id": 2, "name": "Takki", "price": 19.99, "listPrice": 39.90, "onCampaign": true, ... },
  "campaigns": [ { "price": 19.99, "validFrom": "2026-10-12", "validTo": "2026-10-27" } ]
}
```

**GET /api/products/{id}/history**
Tuotteen nimen ja hinnan muutoshistoria. Viimeinen rivi on nykyinen versio.

```json
[
  { "name": "Reppu", "price": 49.90, "validFrom": "2026-10-01T10:00:00", "validTo": "2026-10-15T08:30:00" },
  { "name": "Reppu", "price": 39.90, "validFrom": "2026-10-15T08:30:00", "validTo": "2106-02-07T06:28:15" }
]
```

**GET /api/categories**
Kaikki kategoriat.

```json
[ { "id": 3, "name": "Retkeily", "description": "Teltat ja reput" } ]
```

**GET /api/categories/{id}/products**
Kategorian tuotteet halvimmasta alkaen. Sivutettu tuotelista kuten yllä.


### Asiakas

**POST /api/customers**
Rekisteröinti. Etunimi, sukunimi ja sähköposti pakollisia, sama sähköposti vain kerran. Palauttaa 201 ja asiakkaan.

```json
{ "firstName": "Matti", "lastName": "Meikäläinen", "email": "matti@example.com", "phone": "0401234567" }
```
```json
{
  "id": 100001, "firstName": "Matti", "lastName": "Meikäläinen", "email": "matti@example.com",
  "phone": "0401234567", "loyaltyPoints": 0, "addresses": []
}
```

**GET /api/customers/{id}**
Asiakkaan tiedot, bonuspisteet ja osoitteet. Muoto sama kuin yllä.

**GET /api/customers?email=matti@example.com**
Asiakkaan haku sähköpostilla. Muoto sama kuin yllä.

**PUT /api/customers/{id}**
Tietojen muokkaus, runko sama kuin rekisteröinnissä. Palauttaa päivitetyn asiakkaan.

**DELETE /api/customers/{id}**
Tilin poisto, palauttaa 204. Jos asiakkaalla on tilauksia, henkilötiedot anonymisoidaan koska tilaukset pitää säilyttää.

**GET /api/customers/{id}/orders**
Asiakkaan tilaukset uusin ensin, sivutettu.

```json
{
  "content": [ { "orderId": 200001, "orderDate": "2026-10-17T11:55:49", "status": "PAID", "totalQuantity": 4, "totalAmount": 475.26 } ],
  "page": 0, "size": 20, "totalElements": 1, "totalPages": 1
}
```

**GET /api/customers/{id}/addresses**
Asiakkaan osoitteet.

```json
[ { "id": 100001, "streetAddress": "Mäkitie 1", "postalCode": "01300", "city": "Vantaa", "country": "Finland" } ]
```

**POST /api/customers/{id}/addresses**
Uusi osoite, katuosoite ja kaupunki pakollisia. Palauttaa 201 ja osoitteen.

```json
{ "streetAddress": "Mäkitie 1", "postalCode": "01300", "city": "Vantaa", "country": "Finland" }
```

**PUT /api/customers/{id}/addresses/{addressId}**
Osoitteen muokkaus, runko sama kuin yllä. Palauttaa osoitteen.

**DELETE /api/customers/{id}/addresses/{addressId}**
Osoitteen poisto, palauttaa 204. Jos osoitetta on käytetty tilauksessa, palauttaa 409.


### Tilaukset

Tilat: NEW, PAID, SHIPPED, DELIVERED, CANCELLED ja EXPIRED. Asiakas voi maksaa ja perua, lähetys ja toimitus tehdään ylläpidon puolella.

**POST /api/orders**
Uusi tilaus, tuotteet varataan varastosta heti. Palauttaa 201 ja tilauksen. Jos yhtäkin tuotetta ei ole tarpeeksi, palauttaa 409 eikä mitään tallenneta.

```json
{ "customerId": 100001, "addressId": 100001, "items": [ { "productId": 2, "quantity": 2 }, { "productId": 5, "quantity": 1 } ] }
```
```json
{
  "orderId": 200001, "customerId": 100001, "orderDate": "2026-10-17T11:55:49", "deliveryDate": null,
  "status": "NEW",
  "shippingAddress": { "id": 100001, "streetAddress": "Mäkitie 1", "postalCode": "01300", "city": "Vantaa", "country": "Finland" },
  "items": [
    { "productId": 2, "productName": "Takki", "quantity": 2, "unitPrice": 19.99, "lineTotal": 39.98 },
    { "productId": 5, "productName": "Näyttö", "quantity": 1, "unitPrice": 415.29, "lineTotal": 415.29 }
  ],
  "totalAmount": 455.27
}
```

**GET /api/orders/{id}**
Tilauksen tiedot ja rivit, muoto sama kuin yllä. Toimitusosoite näytetään sellaisena kuin se oli tilaushetkellä.

**GET /api/orders/{id}/history**
Tilauksen tilamuutokset.

```json
[
  { "oldStatus": null, "newStatus": "NEW", "changedAt": "2026-10-17T11:55:49" },
  { "oldStatus": "NEW", "newStatus": "PAID", "changedAt": "2026-10-17T11:56:10" }
]
```

**POST /api/orders/{id}/pay**
Maksaa tilauksen (NEW > PAID). Palauttaa tilauksen, tai 409 jos tilaus ei ole enää NEW tilassa.

**POST /api/orders/{id}/cancel**
Peruu tilauksen ja palauttaa tuotteet varastoon. Onnistuu NEW ja PAID tiloissa, muuten 409. Palauttaa tilauksen.


## Toteutetut ominaisuudet

* Tuotteiden suodatus on tehty Criteria API:lla (`ProductSpecs`). Ehto lisätään vain jos parametri on annettu.
* Tilauksen teko on yksi transaktio. Varasto vähennetään ehdollisella päivityksellä `UPDATE products SET stock_quantity = stock_quantity - ? WHERE id = ? AND stock_quantity >= ?`, joten luku ja vähennys ovat samassa lauseessa eikä kadonnutta päivitystä synny. Jos yksi tuote ei riitä, koko tilaus perutaan. Testattu 15 yhtäaikaisella tilauksella tuotteelle jota oli 3 kpl: 3 onnistui ja saldo jäi nollaan.
* Tuotteet käsitellään aina id järjestyksessä, jolloin kaksi samanaikaista tilausta lukitsee rivit samassa järjestyksessä eikä lukkiumaa synny.
* Maksu tehdään ehdollisella päivityksellä `WHERE status = 'NEW'`, joten jo vanhentunutta tilausta ei voi maksaa.
* Peruutus lukitsee tilauksen ensin (`@Lock(PESSIMISTIC_WRITE)` eli `SELECT ... FOR UPDATE`), jotta ajastettu tapahtuma ei palauta samoja tuotteita varastoon samaan aikaan.
* Tilin poisto anonymisoi henkilötiedot jos asiakkaalla on tilauksia (GDPR).
* Sähköposti ja nimet normalisoidaan ennen tallennusta JPA entity listenerillä (`CustomerListener`).
* Tilausrivit haetaan `JOIN FETCH` kyselyllä, jolloin tuotteet tulevat samassa kyselyssä eikä N+1 ongelmaa synny.
* `orderitems` on linkkitaulu lisäkentillä, joten sille on oma entiteetti yhdistetyllä avaimella (`@EmbeddedId`, `@MapsId`). `customer_profiles` käyttää samaa avainta kuin asiakas (`@OneToOne`, `@MapsId`).
* Näkymät on mapattu `@Immutable` entiteeteiksi. Versiointi, tekstihaku ja hintafunktio ovat native kyselyitä koska JPQL ei tue niitä.


## Tietokanta

Kaikki muutokset ovat tiedostossa `db/webshop.sql`.

### Näkymät

* **v_product_catalog** yhdistää tuotteet, kategoriat, toimittajat ja kampanjahinnat. Kaikki tuotehaut lukevat tätä. Näkymä on MERGE tyyppinen, joten indeksit toimivat sen läpi. Toimittajan yhteystiedot eivät näy.
* **v_order_summary** laskee tilauksen kappalemäärän ja summan. Summat on tehty alikyselyillä GROUP BY:n sijaan, koska GROUP BY tekisi näkymästä TEMPTABLE tyyppisen ja koko tilaustaulu laskettaisiin joka kerta. Nyt yhden asiakkaan tilaukset haetaan indeksin kautta noin 15 ms ajassa 200 000 tilauksella.

### Liipaisimet

* **orders_status_init** ja **orders_status_log** kirjaavat jokaisen tilamuutoksen tauluun `order_status_history`.
* **trg_orders_status_guard** sallii vain oikeat tilasiirtymät ja asettaa toimituspäivän kun tila on DELIVERED.
* **trg_orderitems_before_insert** asettaa rivin hinnan aina kannasta, joten hintaa ei voi syöttää itse. Rivejä voi lisätä vain NEW tilaukseen.
* **trg_orders_loyalty_points** antaa bonuspisteitä (1 piste per euro) kun tilaus toimitetaan.

Lisäksi CHECK rajoitteet estävät negatiivisen hinnan ja saldon sekä nollan tai negatiivisen tilausmäärän. Funktio **fn_effective_price** palauttaa tuotteen hinnan tiettynä päivänä.

### Indeksit

* **idx_email** asiakkaan haku sähköpostilla.
* **idx_products_category_price** (category_id, price) kategorian tuotteet hinnan mukaan.
* **idx_orders_customer_date** (customer_id, order_date) asiakkaan tilaukset uusin ensin.
* **idx_orders_status_date** (status, order_date) ajastettu tapahtuma etsii vanhentuneet tilaukset tällä. Pelkkä status olisi huono indeksi koska yksi arvo kattaa noin kolmanneksen riveistä.
* **ft_products_name_desc** FULLTEXT tekstihakua varten, koska `LIKE '%sana%'` ei käytä tavallista indeksiä.


### Temporaalisuus

* `products` ja `customeraddresses` ovat järjestelmäversioituja (system versioning). Tuotteesta tallentuu nimen ja hinnan historia, varastosaldo on jätetty pois (`WITHOUT SYSTEM VERSIONING`) koska se muuttuu joka tilauksella. Osoitteen historian ansiosta tilauksessa näkyy osoite tilaushetkellä (`FOR SYSTEM_TIME AS OF`), vaikka asiakas olisi muuttanut osoitettaan.
* `product_prices` käyttää kelpoisuusaikaa (`PERIOD FOR valid_time`). `WITHOUT OVERLAPS` estää päällekkäiset kampanjat samalle tuotteelle.
* Tilamuutosten historia on tehty omana historiatauluna liipaisimella.

### Ajastettu tapahtuma

**ev_expire_unpaid_orders** ajetaan minuutin välein. Yli 30 minuuttia maksamatta olleet tilaukset merkitään EXPIRED ja niiden tuotteet palautetaan varastoon. Kaikki tehdään yhdessä transaktiossa ja rivit lukitaan `FOR UPDATE`, jotta samaan aikaan tuleva maksu ei mene ristiin. Vaatii että `event_scheduler` on päällä.

### Tietoturva

Sovellus käyttää omaa käyttäjää `webshop_app`:

* tuotteisiin, kategorioihin ja kampanjahintoihin vain SELECT, tuotteisiin lisäksi UPDATE pelkkään `stock_quantity` sarakkeeseen
* asiakkaisiin ja osoitteisiin SELECT, INSERT, UPDATE ja DELETE
* tilauksiin ja riveihin ei DELETE oikeutta, tilauksia ei poisteta koskaan
* tilahistoriaan ja bonuspisteisiin vain SELECT, niihin kirjoittavat vain liipaisimet
* ei DELETE HISTORY oikeutta, joten versioitujen taulujen historiaa ei voi poistaa

Kaikki kyselyt käyttävät parametreja, joten SQL injektio ei ole mahdollinen. Syötteet tarkistetaan ennen tallennusta. Salasana luetaan ympäristömuuttujasta `DB_PASSWORD`.
