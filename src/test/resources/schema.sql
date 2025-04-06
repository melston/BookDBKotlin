CREATE TABLE books (
    id INT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255),
    file_path VARCHAR(255),
    publisher_id VARCHAR(20),
    is_read BOOLEAN DEFAULT FALSE,
    is_favorite BOOLEAN DEFAULT FALSE
);
