package model;

import listener.RotomListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PokemonGenerationTracker {

    private static final Logger logger = LoggerFactory.getLogger(PokemonGenerationTracker.class);

    private final ArrayList<Map<String, String>> pokemonListByGen;

    public PokemonGenerationTracker() {
        this.pokemonListByGen = new ArrayList<>();
        this.addGen();
    }

    public int addGen() {
        pokemonListByGen.add(new HashMap<>());
        return pokemonListByGen.size() - 1;
    }

    public void addPokemon(int gen, String pokemon, String link) {
        pokemonListByGen.get(gen).put(pokemon, link);
    }

    public void addPokemon(String pokemon, String link) {
        addPokemon(pokemonListByGen.size() - 1, pokemon, link);
    }

    public Map<String, String> getPokemonByGen(int startGen, int endGen) {
        Map<String, String> finalMap = new HashMap<>();
        for (int i = startGen; i <= endGen; ++i) {
            if (i >= pokemonListByGen.size()) {
                break;
            }
            else {
                finalMap.putAll(pokemonListByGen.get(i));
            }
        }
        return finalMap;
    }

    public void trimEmptyGens() {
        int prevCount = 0;
        do {
            prevCount = pokemonListByGen.get(pokemonListByGen.size() - 1).size();
            if (prevCount == 0) {
                pokemonListByGen.remove(pokemonListByGen.size() - 1);
            }
        } while (prevCount <= 0);
    }

    public void print() {
        for (int i = 0; i < pokemonListByGen.size(); ++i) {
            logger.info("Gen " + i);
            for (String pokemon : pokemonListByGen.get(i).keySet()) {
                logger.info(pokemon);
            }
            logger.info("");
        }
    }

    public int genCount() {
        return  pokemonListByGen.size();
    }

    public int pokemonCount(int gen) {
        return pokemonListByGen.get(gen).size();
    }
}
