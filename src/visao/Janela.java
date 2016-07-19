package visao;

import java.util.Scanner;
import controle.SistemaOperacional;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.chart.*;
import javafx.scene.chart.PieChart.Data;
import javafx.scene.control.Label;
import javafx.scene.Group;

/**
 * Classe que produz o gráfico das taxas de hit e miss, e recebe no inicio os
 * valores de tamanho das memórias e das páginas
 * 
 * @author edupooch
 *
 */
public class Janela extends Application {

	public static Data miss;
	public static Data hit;
	private int hits;
	private int misses;
	public Label taxa;
	public static ObservableList<Data> pieChartData;

	@Override
	public void start(Stage stage) {

		int tamanhoPagina = 0;
		int tamanhoFisica = 0;
		int tamanhoVirtual = 0;

		Scanner leitor = new Scanner(System.in);

		boolean confirma = false;
		while (!confirma) {
			System.out.println("--------------------------------------------------");
			System.out.println("Digite o tamanho da Página e do Frame (KB):");
			tamanhoPagina = leitor.nextInt();
			System.out.println("Digite o tamanho da Memória Física (KB):");
			tamanhoFisica = leitor.nextInt();
			System.out.println("Digite o tamanho da Memória Virtual (KB):");
			tamanhoVirtual = leitor.nextInt();

			if (tamanhoPagina > tamanhoFisica) {
				System.out.println("Tamanho da página maior do que a Memória Física!");
			} else if (tamanhoPagina > tamanhoVirtual) {
				System.out.println("Tamanho da página maior do que a Memória Virtual!");
			} else if (tamanhoPagina < 1 || tamanhoFisica < 1 || tamanhoVirtual < 1) {
				System.out.println("Os tamanhos devem ser maiores do que 0");
			} else {
				confirma = true;
			}
		}

		int numeroPosicoesFisica = (Integer) tamanhoFisica / tamanhoPagina;
		System.out.println("A memória física tem " + numeroPosicoesFisica + " posições");
		int numeroPosicoesVirtual = (Integer) tamanhoVirtual / tamanhoPagina;
		System.out.println("A memória lógica tem " + numeroPosicoesVirtual + " posições");

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		SistemaOperacional sistema = new SistemaOperacional(numeroPosicoesVirtual, numeroPosicoesFisica, tamanhoPagina);
		Thread threadSistema = new Thread(sistema);

		threadSistema.start();

		Scene scene = new Scene(new Group());
		stage.setTitle("Simulador de Paginação");
		stage.setWidth(520);
		stage.setHeight(520);

		pieChartData = FXCollections.observableArrayList(miss = new PieChart.Data("Miss", 0), new PieChart.Data("", 0),
				hit = new PieChart.Data("Hit", 0));

		final PieChart grafico = new PieChart(pieChartData);
		grafico.setTitle("Taxa de Hit e Miss");
		grafico.setLayoutY(30);

		((Group) scene.getRoot()).getChildren().add(grafico);

		pieChartData.remove(1);
		stage.setScene(scene);
		stage.show();

	}

	public static void main(String[] args) {
		launch(args);
	}

	public void setTaxaText(String texto) {
		this.taxa.setText(texto);

	}
}