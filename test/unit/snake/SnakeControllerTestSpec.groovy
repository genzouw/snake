package snake

import spock.lang.*

class SnakeControllerTestSpec extends Specification {
    static def date = { x ->
        Date.parse("yyyy MM dd", x)
    }

    @Unroll
    def "replaceAllOnRules"() {
        setup:
            def target = new SnakeController()

        expect:
            expect== target.replaceAllOnRules(
                sentence,
                rules
            )

        where:
            sentence            | rules                         | expect
            "1112322123"        | [["11", "aa"], ["22", "bb"]]  | "aa123bb123"
    }

    @Unroll
    def "pickupCategories"() {
        setup:
            def target = new SnakeController()

        expect:
            expect == target.pickupCategories(
                row
            )

        where:
            row                                     | expect
            ["aaa:bbb"]                             | [ "aaa:bbb" ]
            ["aaa:bbb\naaa:bbb:ccc"]                | [ "aaa:bbb" ]
            ["aaa\naaaccccc"]                       | []
            ["aaa:bbb\naaa:bbb:ccc\naaa:bbb" ]      | [ "aaa:bbb" ]
            ["aaa:bbb\naaa:ccc:ddd\naaa:bbb:cccc" ] | [ "aaa:bbb", "aaa:ccc" ]
    }

    @Unroll
    def "pickupItemCodesInSameCategories"() {
        setup:
            def target = new SnakeController()

        expect:
            expect == target.pickupItemCodesInSameCategories(
                categories, categorizedMap
            )?.sort()

        where:
            expect                     | categories | categorizedMap
            []                         | []         | ["1":["c001"] , "2":["c002"], "3":["c003"]]
            [ "c001" ]                 | ["1"]      | ["1":["c001"] , "2":["c002"], "3":["c003"]]
            [ "c002" ]                 | ["2"]      | ["1":["c001"] , "2":["c002"], "3":["c003"]]
            [ "c001", "c003" ]         | ["1", "3"] | ["1":["c001"] , "2":["c002"], "3":["c003"]]
            [ "b001" , "c001", "c002"] | ["1", "2"] | ["1":["c001", "b001"] , "2":["c002", "b001"], "3":["c003"]]
    }

    @Unroll
    def "pickupCategoryParts"() {
        setup:
            def target = new SnakeController()

        expect:
            expect == target.pickupCategoryParts(
                row
            )

        where:
            row                                     | expect
            [""]                                    | []
            ["aaa:bbb"]                             | [ "aaa", "bbb" ]
            ["aaa:bbb\naaa:bbb:ccc"]                | [ "aaa", "bbb", "ccc" ]
            ["aaa\naaaccccc"]                       | [ "aaa", "aaaccccc" ]
            ["aaa:bbb\naaa:bbb:ccc\naaa:bbb" ]      | [ "aaa", "bbb", "ccc" ]
            ["aaa:bbb\naaa:ccc:ddd\naaa:bbb:cccc" ] | [ "aaa", "bbb", "ccc", "cccc", "ddd" ]
    }

}
