import kotlin.random.Random

var money = HashMap<Int, Int>()
val cards = ArrayList<Card>()

val deck = List(52) { i -> generateCard(i) }

val playersCards = HashMap<Int, ArrayList<Card>>()
val bets = HashMap<Int, Int>()

val dealerCards = ArrayList<Card>()

fun generateCard(seed: Int): Card {
    val a = seed % 13
    val b = seed / 13
    return Card(
        when (a) {
            0 -> "Ace"
            10 -> "Jack"
            11 -> "Queen"
            12 -> "King"
            else -> (a + 1).toString()
        },
        when (b) {
            0 -> "Spades"
            1 -> "Hearts"
            2 -> "Clubs"
            else -> "Diamonds"
        },
        a + 1
    )
}

const val getPlayerCount = "How many players? (Must be between 1 and 7)"
const val badPlayerCount = "Number of players must be between 1 and 7."

const val getNumOfDecks = "How many decks? (Must be between 1 and 8)"
const val badNumOfDecks = "Number of decks must be between 1 and 8."

const val getAmount = "Enter an amount to bet (between \$5 and \$20):"
const val badAmount = "Entered amount was not a number between $5 and $20."

const val getOption = "What will you do?\n1. Stand\n2. Hit"
const val badOption = "Entry was not a valid option."

data class Card(val faceValue: String, val suite: String, val identifyingValue: Int) {
    override fun toString(): String {
        return "$faceValue of $suite"
    }
}

lateinit var random: Random

fun main() {
    var gameEnded = false

    val time = System.currentTimeMillis()
    println(time)
    random = Random(time)

    val lastRoundResults = HashMap<Int, Int>()

    val playerCount = getValidInput(getPlayerCount, badPlayerCount) { it in 1..7 }
    val numOfDecks = getValidInput(getNumOfDecks, badNumOfDecks) { it in 1..8 }
    initDeck(numOfDecks)

    for (i in 1..playerCount) {
        playersCards[i] = ArrayList()
        money[i] = 1000
        bets[i] = 0
    }


    while (!gameEnded) {
        for (i in playersCards.keys) {
            println("Player $i, you have $${money[i]}.")

            if (lastRoundResults.containsKey(i) && lastRoundResults[i] == 3) {
                println("Player $i, since you pushed last round, your bet of ${bets[i]} remains.")
            } else {
                val amountToBet = getValidInput(getAmount, badAmount) { it in 5..20 }
                money[i] = money[i]!!.minus(amountToBet)
                bets[i] = amountToBet
            }
            //Quitting?

        }

        for (i in playersCards.keys) {
            playersCards[i]!!.add(dealCard())
            playersCards[i]!!.add(dealCard())

            println("Player $i got ${playersCards[i]}.")
        }

        dealerCards.add(dealCard())
        dealerCards.add(dealCard())
        println("The dealer shows ${dealerCards[dealerCards.lastIndex]}.")

        for (i in playersCards.keys) {
            var option = 0
            var hits = 0
            while (option != 1) {
                val playerCards = playersCards[i]
                val total = playerCards!!.getTotal()
                println("Player $i, your cards are $playerCards and your total is $total.")

                if (total > 21) {
                    println("You have gone bust.")
                    option = 1
                } else if (hits == 0 && total == 21) {
                    println("You have Blackjack.")
                    option = 1
                } else {
                    option = getValidInput(getOption, badOption) { it in 1..2 }

                    if (option == 2) {
                        val card = dealCard()
                        hits++

                        println("You drew $card.")
                        playerCards.add(card)
                    }
                }
            }
        }

        println("The dealer reveals ${dealerCards[dealerCards.lastIndex - 1]}")
        println("The dealer has a total of ${dealerCards.getTotal()}")

        if (dealerCards.getTotal() <= 16) {
            dealerCards.add(dealCard())
            println("The dealer hit, and drew ${dealerCards[dealerCards.lastIndex]}")
            println("The dealer's now has a total of ${dealerCards.getTotal()}")
        }

        val dealerTotal = dealerCards.getTotal()

        if (dealerTotal > 21) {
            println("The dealer went bust!")
        }
        playersCards.keys.forEach {
            lastRoundResults[it] = distributeWinnings(it, dealerTotal)
            playersCards[it]!!.clear()

        }

        dealerCards.clear()
        println()
    }
}


fun initDeck(numOfDecks: Int) {
    cards.clear()
    //Shuffle deck?
    repeat(numOfDecks) {
        cards.addAll(deck)
    }
}

fun dealCard(): Card {
    return cards.getCard()
}

fun distributeWinnings(playerNumber: Int, dealerTotalValue: Int): Int {
    val playerCards = playersCards[playerNumber]
    val totalValue = playerCards!!.getTotal()
    val hands = playerCards.size - 2

    var moneyPaid = 0
    var returnValue = 0
    if (totalValue == 21 && hands == 0) {
        moneyPaid = (bets[playerNumber]!! * 2.5).toInt()
        println("Player $playerNumber had Blackjack and got a payout of $$moneyPaid.")
        returnValue = 1

    } else if ((totalValue in (dealerTotalValue + 1)..20) or (totalValue <= 21 && dealerTotalValue > 21)) {
        moneyPaid = bets[playerNumber]!! * 2
        println("Player $playerNumber had a total of $totalValue and got a payout of $$moneyPaid.")
        returnValue = 1

    } else if (totalValue < 21 && totalValue == dealerTotalValue) {
        println("Player $playerNumber had a push with a total of $totalValue.")
        returnValue = 3

    } else if (totalValue < dealerTotalValue) {
        println("Player $playerNumber lost with a score of $totalValue, which was less than the dealer's score.")

    } else {
        println("Player $playerNumber went bust, with a total of $totalValue.")

    }

    money[playerNumber] = money[playerNumber]!! + moneyPaid

    return returnValue
}

fun getValidInput(outputString: String, incorrectString: String, conditional: (Int) -> Boolean): Int {
    var enteredValidEntry = false
    var input = -1
    while (!enteredValidEntry) {
        println(outputString)
        val next = readLine()
        input = try {
            next?.toInt() ?: -1
        } catch (e: NumberFormatException) {
            -1
        }

        enteredValidEntry = conditional(input)

        if (!enteredValidEntry) {
            println(incorrectString)
        }
    }

    return input
}



fun ArrayList<Card>.getCard(): Card {
    val i = (0 until size).random(random)
    val card = get(i)
    removeAt(i)
    return card
}

fun ArrayList<Card>.getTotal(): Int {
    var total = 0
    var aces = 0
    for (card in this) {
        total += if (card.identifyingValue > 10) {
            10
        } else {
            card.identifyingValue
        }
        if (card.identifyingValue == 1) {
            aces++
        }
    }

    //if adding the ace keeps the total under 21, ace = 11
    for (i in 1..aces) {
        if (total + 10 <= 21) {
            total += 10
        }
    }
    return total
}
