package sensor;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class GRAPHE {
    private static final String FILE_NAME = "sensor_values.txt";
    private static final Object lock = new Object();
    private static PrintWriter writer;
    private static XYSeries temperatureSeries;
    private static XYSeries pressureSeries;
    private static XYSeries humiditySeries;

    public static void main(String[] args) {
        // Initialisation des séries pour le graphe
        temperatureSeries = new XYSeries("Température");
        pressureSeries = new XYSeries("Pression");
        humiditySeries = new XYSeries("Humidité");

        // Création d'un objet Random pour générer des nombres aléatoires
        Random random = new Random();

        // Création des threads pour la température, la pression et l'humidité
        Thread temperatureThread = new Thread(new MesureThread("Température", random, 20, 35, temperatureSeries));
        Thread pressureThread = new Thread(new MesureThread("Pression", random, 900, 1000, pressureSeries));
        Thread humidityThread = new Thread(new MesureThread("Humidité", random, 30, 100, humiditySeries));

        try {
            // Initialisation de l'écrivain
            writer = new PrintWriter(new FileWriter(FILE_NAME, true));

            // Démarrage des threads
            temperatureThread.start();
            pressureThread.start();
            humidityThread.start();

            // Affichage du graphe
            afficherGraphe();

            // Attendre que tous les threads se terminent
            temperatureThread.join();
            pressureThread.join();
            humidityThread.join();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    private static void afficherGraphe() {
        // Création du dataset
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(temperatureSeries);
        dataset.addSeries(pressureSeries);
        dataset.addSeries(humiditySeries);

        // Création du graphe
        JFreeChart chart = ChartFactory.createXYLineChart(
                "Mesures Capteur",
                "Temps",
                "Valeur",
                dataset
        );

        // Création du panneau de graphe
        ChartPanel chartPanel = new ChartPanel(chart);

        // Création de la fenêtre
        JFrame frame = new JFrame("Mesures Capteur");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(chartPanel, BorderLayout.CENTER);
        frame.setSize(800, 600);
        frame.setVisible(true);
    }

    public static void sauvegarderMesureDansFichier(String mesureType, double mesureValue, String timestamp, XYSeries series) {
        synchronized (lock) {
            // Ajout de la mesure à la série du graphe
            try {
                Date date = new SimpleDateFormat("yyyyMMdd_HHmmss").parse(timestamp);
                series.add(date.getTime(), mesureValue);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            // Écriture des valeurs dans le fichier
            if (writer != null) {
                writer.println("Timestamp: " + timestamp);
                writer.println(mesureType + " : " + mesureValue);
                System.out.println("La mesure a été sauvegardée dans le fichier et ajoutée au graphe.");
            } else {
                System.err.println("Erreur : PrintWriter est null.");
            }
        }
    }
}

class MesureThread implements Runnable {
    private String mesureType;
    private Random random;
    private double minValue;
    private double maxValue;
    private XYSeries series;

    public MesureThread(String mesureType, Random random, double minValue, double maxValue, XYSeries series) {
        this.mesureType = mesureType;
        this.random = random;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.series = series;
    }

    @Override
    public void run() {
        while (true) {
            double mesureValue = minValue + (random.nextDouble() * (maxValue - minValue));

            // Obtention du timestamp actuel
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

            // Affichage et sauvegarde dans le fichier
            System.out.println(mesureType + " : " + mesureValue);
            GRAPHE.sauvegarderMesureDansFichier(mesureType, mesureValue, timestamp, series);

            try {
                // Pause de 5 secondes entre chaque mesure
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

