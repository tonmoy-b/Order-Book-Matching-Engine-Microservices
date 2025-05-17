-- make sure the table 'order'  exists
--CREATE TABLE IF NOT EXISTS order_item
--(
--    order_id             UUID                PRIMARY KEY,
--    client_id            VARCHAR(255)         NOT NULL,
--    asset                VARCHAR(255)         NOT NULL,
--    order_time           TIMESTAMP            NOT NULL,
--    order_type           VARCHAR(255)         NOT NULL,
--    amount               numeric              NOT NULL,
--    volume               integer              NOT NULL,
--);


--multiple UUIDs generated from https://www.uuidgenerator.net/
--1c820d25-997d-4470-9e10-8014d42487d0
--ecde436c-7152-417a-b250-982830251aa8
--4080bd6f-b178-4b6e-b4ce-c9dfa7aa0f03
--7f60cca5-3ce5-4d85-8efb-418cb6ecf3c0
--0720873d-5187-4457-be39-9e5ff112547c


INSERT INTO order_item (order_id, client_id, asset, order_time, order_type, amount, volume)
SELECT '1c820d25-997d-4470-9e10-8014d42487d0',
       'atlas',
       'aapl',
       '2025-05-17 00:00:00.000',
       'bid',
       123.34,
       21
WHERE NOT EXISTS (SELECT 1
                  FROM order_item
                  WHERE order_id = '1c820d25-997d-4470-9e10-8014d42487d0');
INSERT INTO order_item (order_id, client_id, asset, order_time, order_type, amount, volume)
SELECT 'ecde436c-7152-417a-b250-982830251aa8',
       'atlas',
       'aapl',
       '2025-05-17 00:00:00.001',
       'bid',
       123.34,
       21
WHERE NOT EXISTS (SELECT 1
                  FROM order_item
                  WHERE order_id = 'ecde436c-7152-417a-b250-982830251aa8');
INSERT INTO order_item (order_id, client_id, asset, order_time, order_type, amount, volume)
SELECT '4080bd6f-b178-4b6e-b4ce-c9dfa7aa0f03',
       'atlas',
       'bmp',
       '2025-05-17 00:00:01.000',
       'bid',
       123.34,
       21
WHERE NOT EXISTS (SELECT 1
                  FROM order_item
                  WHERE order_id = '4080bd6f-b178-4b6e-b4ce-c9dfa7aa0f03');
INSERT INTO order_item (order_id, client_id, asset, order_time, order_type, amount, volume)
SELECT '7f60cca5-3ce5-4d85-8efb-418cb6ecf3c0',
       'atlas',
       'aapl',
       '2025-05-17 00:00:02.000',
       'ask',
       123.34,
       21
WHERE NOT EXISTS (SELECT 1
                  FROM order_item
                  WHERE order_id = '7f60cca5-3ce5-4d85-8efb-418cb6ecf3c0');
INSERT INTO order_item (order_id, client_id, asset, order_time, order_type, amount, volume)
SELECT '0720873d-5187-4457-be39-9e5ff112547c',
       'atlas',
       'aapl',
       '2025-05-17 00:00:03.000',
       'ask',
       123.34,
       21
WHERE NOT EXISTS (SELECT 1
                  FROM order_item
                  WHERE order_id = '0720873d-5187-4457-be39-9e5ff112547c');
