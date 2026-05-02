<?php
$pdo = new PDO('mysql:host=127.0.0.1;dbname=eyetwin_platform', 'root', '');
$tables = ['tournoi', 'matchs', 'matches', 'match'];

foreach($tables as $t) {
    try {
        echo "TABLE: $t\n";
        $stmt = $pdo->query("DESCRIBE `$t`");
        while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
            echo $row['Field'] . " - " . $row['Type'] . "\n";
        }
    } catch(Exception $e) {
        echo "failed: " . $e->getMessage() . "\n";
    }
}
